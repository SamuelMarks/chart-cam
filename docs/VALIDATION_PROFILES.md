# Validation & Terminology Profiles

## Validation
ChartCam enforces structural validation on FHIR resources (e.g. `Patient`, `Questionnaire`) via `io.healthplatform.chartcam.validation.FhirValidator`. This replaces custom if/else logic in the ViewModels.

### Patient Profile
- `name` must be present.
- A patient must have at least one `given` name.
- A patient must have a `family` name.
- `identifier` (e.g., MRN) must be present.

### Questionnaire Profile
- `title` must be populated.
- At least one `item` must be present.
- Each `item` must have a valid `linkId` and `text`.

## Terminology
ChartCam relies on native FHIR terminology resources rather than hardcoded URLs.
- **LOINC** (`http://loinc.org`): Generated using `TerminologyService.getLoincCodeSystem()` and `getLoincCoding()`.
- **SNOMED CT** (`http://snomed.info/sct`): Generated using `TerminologyService.getSnomedCodeSystem()`.