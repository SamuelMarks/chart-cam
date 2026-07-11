# ChartCam FHIR Forms Architecture

This document details the architectural decisions, structural flow, and implementation specifics of the dynamic FHIR Forms subsystem in ChartCam, encompassing the Form Builder, Form Viewer, and Result-Viewer.

## Overview

ChartCam leverages the [HL7 FHIR Structured Data Capture (SDC)](https://hl7.org/fhir/uv/sdc/) implementation guide to render robust, dynamic, and fully native Jetpack Compose forms directly from standard FHIR `Questionnaire` resources. 

The architecture is designed to be:
1. **Dynamic & Data-Driven:** The UI is completely generated at runtime based on the `Questionnaire` structure.
2. **FHIR-Native:** Core business logic relies entirely on the FHIR standard, completely avoiding proprietary JSON schemas or intermediately decoupled UI models.
3. **Multiplatform:** Built on Compose Multiplatform, ensuring identical visual and logical rendering across Android, iOS, Web, and Desktop.

## Subsystems

### 1. Form Builder (`QuestionnaireBuilderScreen` & `QuestionnaireBuilderViewModel`)

The Form Builder allows users to construct custom forms. 

- **State Management:** The ViewModel manages an ordered list of `BuilderItem` objects, which serve as intermediate UI states for tracking labels, dropdown options, and validation states (e.g., detecting empty labels or missing options).
- **Serialization:** Upon saving, the `buildQuestionnaire()` method translates the intermediate `BuilderItem` list into a compliant FHIR `Questionnaire` resource. 
  - Standard input types (Text, Boolean, Decimal) map directly to `QuestionnaireItemType`.
  - Specific Compose UI widgets (e.g., Sliders vs. Numeric Inputs) are disambiguated by embedding standard SDC Extensions (e.g., `itemControl`, `minValue`, `maxValue`) into the generated items.
- **Validation:** Validation is performed *before* FHIR conversion. A form cannot be serialized or saved if it contains structurally invalid components.

### 2. Form Viewer (`SdcQuestionnaireForm` & `RenderQuestionnaireItem`)

The `SdcQuestionnaireForm` component consumes a FHIR `Questionnaire` and a transient state map of user answers, outputting a fully interactive UI. It acts as an equivalent KMP SDC engine.

- **Recursive Rendering:** `RenderQuestionnaireItem` is called recursively. If it encounters a `QuestionnaireItemType.Group`, it renders an ElevatedCard and recursively maps over the nested `item`s.
- **Strict SDC Abstractions:** Rather than parsing raw extension URLs inside the UI components, we utilize `SdcExtensions.kt`. These strongly-typed Kotlin extensions safely extract data like `.isHidden()`, `.getItemControl()`, and range boundaries.
- **Animated Visibility & Conditional Logic:** `enableWhen` conditions dictate visibility.
- **Calculated Expressions:** The `SdcEvaluator` parses `sdc-questionnaire-calculatedExpression` extensions to automatically evaluate basic math operations and populate answers dynamically based on user input.
- **Auto-Generation of Responses:** The engine now automatically generates and emits `QuestionnaireResponse` resources whenever user inputs are updated, keeping the ViewModel clean and FHIR-native.

## Custom Questionnaire SDC Guidelines
To ensure forms render correctly within the ChartCam `SdcQuestionnaireForm` engine, custom `Questionnaire` resources must adhere to these structural guidelines:

1.  **Item Control Codes:** Use the `http://hl7.org/fhir/StructureDefinition/questionnaire-itemControl` extension to specify widget types:
    *   `text-box` for multiline text areas.
    *   `slider` for range sliders.
2.  **Calculated Expressions:** Apply the `http://hl7.org/fhir/uv/sdc/StructureDefinition/sdc-questionnaire-calculatedExpression` extension on an item. The inner `expression` must have the language `text/fhirpath` (or be a simple arithmetic string in our prototype evaluator) containing variables mapped to sibling `linkId`s (e.g., `%weight / (%height * %height)`).
3.  **Conditional Rendering:** Use `enableWhen` arrays on items. The `question` must point to a valid `linkId` in the same form, and `operator` and `answer` must be populated.
4.  **Range Constraints:** Use `http://hl7.org/fhir/StructureDefinition/minValue` and `maxValue` extensions on integer/decimal types to enforce validation and dictate slider boundaries.

### 3. Result-Viewer (Read-Only Mode)

The Result-Viewer leverages the exact same `SdcQuestionnaireForm` component but is passed a `readOnly = true` flag.

- **Explicit Empty States:** Instead of dropping unanswered questions from the hierarchy, the UI renders them with a localized "Not answered" label, ensuring visual alignment and contextual clarity.
- **Inline Attachments:** Photos captured during the encounter are linked via `DocumentReference` resources. The form matches the document context back to the `linkId` of the question and renders the thumbnails directly inline using a Compose Grid.

## Serialization (`QuestionnaireResponseGenerator`)

The `QuestionnaireResponseGenerator` object isolates the complex logic required to convert the internal unstructured Compose answer map (`Map<String, Any>`) into a strict, validated, standard FHIR `QuestionnaireResponse` resource.

- **Type Safety:** The generator matches the source `Questionnaire`'s `QuestionnaireItemType` and safely casts/parses the raw state value into the precise `QuestionnaireResponse.Item.Answer.Value` type (e.g., `FhirDate`, `FhirDateTime`, `BigDecimal`, `Boolean`).
- **Hierarchy Preservation:** Nested groups and repeating items are seamlessly maintained in the resulting `QuestionnaireResponse`.
