# Accessibility (a11y) Guidelines

## SDC Forms
- All auto-rendered `Questionnaire` items must map their `text` property to the Compose component's semantic label (`contentDescription`).
- `SdcQuestionnaireForm` actively merges descendants and associates the Question string directly with the input, allowing screen readers (TalkBack/VoiceOver) to correctly vocalize the question when the input is focused.
- Form inputs inherit `MaterialTheme.typography` ensuring adequate scaling for users with "Large Text" accessibility settings.
- Color contrast ratios adhere to standard Material 3 Guidelines (`onSurface` vs `surface`).

## Custom Components
- `LevelerOverlay` communicates its "Level" or "Tilted" status via `LiveRegionMode.Polite` through its root `semantics` block. This allows the state change to be read out continuously to the visually impaired as they align their camera.
- `CameraPreview` surfaces `contentDescription = "Camera Preview"` so users understand the functional region of the screen.

## Text Scaling
All text representations of FHIR data (such as MRN, Birth Date, Form Titles) rely on `MaterialTheme.typography` scaled sp units, guaranteeing reflow without clipping when the user device has magnified accessibility settings.