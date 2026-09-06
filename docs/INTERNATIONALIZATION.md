# Internationalization (i18n)

## Supported Locales
ChartCam supports multiple languages:
- **English (`en`)**: Default locale, LTR layout.
- **Spanish (`es`)**: Latin American / European Spanish, LTR layout.
- **Japanese (`ja`)**: 日本語, LTR horizontal layout.
- **Hebrew (`he`)**: עברית, full Right-to-Left (RTL) layout with mirrored navigation and controls.
- **Traditional Chinese (`zh` / `zh-TW` / `zh-Hant`)**: 繁體中文, supporting both standard horizontal layout and traditional East Asian vertical column-only layout (直書 / 豎排).

Language resources are handled via Compose Multiplatform `Res.string`.

## Right-to-Left (RTL) Script Handling
For languages written right-to-left (such as Hebrew `he` and Arabic `ar`):
- `isRtlLanguage(language: String)` detects RTL language codes (`"he"`, `"iw"`, `"ar"`, `"fa"`, `"ur"`, `"yi"`).
- `getLayoutDirectionForLanguage(language: String)` returns `LayoutDirection.Rtl` when an RTL locale is active and `LayoutDirection.Ltr` otherwise.
- The application root binds `LocalLayoutDirection` dynamically based on `currentLanguageState`:
  ```kotlin
  val currentLang by currentLanguageState.collectAsState()
  CompositionLocalProvider(LocalLayoutDirection provides getLayoutDirectionForLanguage(currentLang)) {
      AppTheme { ... }
  }
  ```
- All standard layouts, rows, start/end paddings, and auto-mirrored icons (`Icons.AutoMirrored.*`) dynamically flip to adhere to RTL writing direction.

## Traditional Chinese Vertical Column Writing System (直書 / 豎排)
In traditional Chinese typography, text is historically and traditionally written in vertical columns:
- Glyphs stack vertically from top to bottom within each column.
- Columns progress from right to left (`vertical-rl`), meaning the reader begins at the top of the rightmost column and moves down, then proceeds to the column on the left.
- ChartCam provides dedicated composables in `io.healthplatform.chartcam.ui.components`:
  - `splitTextIntoVerticalColumns(text: String, maxCharsPerColumn: Int)`: Segments text into vertical column runs respecting line breaks and column length.
  - `VerticalColumnText`: Renders glyphs stacked vertically in columns that progress horizontally from right to left (or configurable direction) with horizontal scrolling.
  - `TraditionalChineseVerticalBanner`: Clinical banner card presenting titles and slogans in traditional vertical column format with an interactive toggle between vertical and horizontal presentation.
  - `isTraditionalChinese(language: String)` and `isVerticalTextLanguage(language: String)` helpers to detect when vertical layout features should be enabled.

## Adding a New Locale
1. Create a new `values-<locale_code>` directory inside `chartCam/src/commonMain/composeResources/`. For example: `values-fr`.
2. Add a `strings.xml` file mapping the English string keys to their translated values.
3. Update `scripts/test_i18n.py` to add the new locale to the automated validation suite.
4. Update the `LoginScreen` and `PatientListScreen` language switcher options to include the new language.

## Localized Clinical Resources
ChartCam loads localized `Questionnaire` definitions locally from bundled templates.
- Localized questionnaires are selected based on the active locale.
- The current application language is read from `io.healthplatform.chartcam.ui.currentLanguageState.value`.

## FHIR Date & Time Formatting
All FHIR Dates and DateTimes are processed using the platform-specific implementations of `formatLocalizedDate(fhirDate: String)` in `io.healthplatform.chartcam.utils.DateFormatter`. This ensures UI consistency across all platforms and respects system locale settings (e.g. `MM/DD/YYYY` vs `DD-MM-YYYY`).