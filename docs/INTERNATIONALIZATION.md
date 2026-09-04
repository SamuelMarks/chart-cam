# Internationalization (i18n)

## Supported Locales
ChartCam supports multiple languages including English (`en`), Spanish (`es`), and Japanese (`ja`). Language resources are handled via Compose Multiplatform `Res.string`.

## Adding a New Locale
1. Create a new `values-<locale_code>` directory inside `chartCam/src/commonMain/composeResources/`. For example: `values-fr`.
2. Add a `strings.xml` file mapping the English string keys to their translated values.
3. Update the `LoginScreen` and `PatientListScreen` language switcher options to include the new language.

## Localized Clinical Resources
ChartCam loads localized `Questionnaire` definitions locally from bundled templates.
- Localized questionnaires are selected based on the active locale.
- The current application language is read from `io.healthplatform.chartcam.ui.currentLanguageState.value`.

## FHIR Date & Time Formatting
All FHIR Dates and DateTimes are processed using the platform-specific implementations of `formatLocalizedDate(fhirDate: String)` in `io.healthplatform.chartcam.utils.DateFormatter`. This ensures UI consistency across all platforms and respects system locale settings (e.g. `MM/DD/YYYY` vs `DD-MM-YYYY`).