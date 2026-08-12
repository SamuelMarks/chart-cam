# TODO_PLAN.md: Questionnaire Management Feature

This document outlines the actionable steps to add the Questionnaire management feature, ensuring we reuse existing functionality (such as navigation, repositories, and platform services) within the `ChartCam` application.

## 1. Navigation Updates
- [x] Open `chartCam/src/commonMain/kotlin/io/healthplatform/chartcam/navigation/Routes.kt`.
- [x] Define a new static route for the questionnaire list (e.g., `const val QUESTIONNAIRE_LIST = "/questionnaires"`).
- [x] Open `chartCam/src/commonMain/kotlin/io/healthplatform/chartcam/navigation/AppNavigation.kt`.
- [x] Add a `composable(Routes.QUESTIONNAIRE_LIST)` block.
- [x] Inside the new composable route, instantiate the future `QuestionnaireListScreen` and pass the existing `questionnaireRepository` from `AppNavigation`.

## 2. Patient List Screen UI Update
- [x] Open `chartCam/src/commonMain/kotlin/io/healthplatform/chartcam/ui/PatientListScreen.kt`.
- [x] Update the `PatientListScreen` signature (or pass a new lambda `onNavigateToQuestionnaires: () -> Unit`).
- [x] Ensure `AppNavigation.kt` passes this lambda to `PatientListScreen` to perform `navController.navigate(Routes.QUESTIONNAIRE_LIST)`.
- [x] In `PatientListScreen.kt`, locate the Top App Bar `DropdownMenu` block (around the `showAboutDialog` interaction).
- [x] Add a new `DropdownMenuItem` with the text "Questionnaires" directly above or below the "About" item.
- [x] Set its `onClick` handler to dismiss the menu (`showMenu = false`) and trigger the `onNavigateToQuestionnaires` callback.

## 3. Create `QuestionnaireListScreen.kt`
- [x] Create a new file: `chartCam/src/commonMain/kotlin/io/healthplatform/chartcam/ui/QuestionnaireListScreen.kt`.
- [x] Define the composable: `@Composable fun QuestionnaireListScreen(questionnaireRepository: QuestionnaireRepository, onBack: () -> Unit)`.
- [x] **UI Layout:** Use a `Scaffold` and reuse standard material design components similar to `PatientListScreen` (TopAppBar with a back navigation arrow).
- [x] **State Management:** Use `remember` and `mutableStateOf` to store the list of questionnaires. Fetch them using `questionnaireRepository.getAvailableQuestionnaires()`.
- [x] **List Display:** Use a `LazyColumn` to render the list of available questionnaires showing their `title` and `id`.

## 4. Implement "Create Questionnaire" Functionality
- [x] In `QuestionnaireListScreen.kt`, add a `FloatingActionButton` (FAB) to the `Scaffold` to trigger the creation of a new questionnaire.
- [x] Create states for the creation dialog: `showCreateDialog`, `newTitle`, `newPhotosCount`, `newLabels`.
- [x] Reuse the `AlertDialog` styling (as seen in `PatientListScreen`'s delete confirmation or about dialog) to build a "Create Questionnaire" form.
- [x] Add `OutlinedTextField` inputs for:
  - Title
  - Number of Photos (keyboard type Number)
  - Labels (comma-separated string)
- [x] On dialog confirmation:
  - Call `questionnaireRepository.createQuestionnaire(title, photos, labels)`.
  - Refresh the list of questionnaires from the repository.
  - Close the dialog.

## 5. Implement "Share Questionnaire" Functionality
- [x] In `QuestionnaireListScreen.kt`, initialize the existing platform share service: `val shareService = remember { createShareService() }`.
- [x] Within the `LazyColumn` item view for each questionnaire, add an `IconButton` with a sharing icon (e.g., `Icons.Default.Share`).
- [x] In the `onClick` event of the Share button, construct a shareable text string containing the questionnaire details (e.g., extracting the title, ID, and list of items/questions).
- [x] Call `shareService.shareText(formattedString)` to trigger the native share sheet.

## 6. Strings and Resources (Clean-up)
- [x] Add necessary string resources to `strings.xml` (or `Res.string` equivalents in Compose Resources) for "Questionnaires", "Create Questionnaire", "Title", "Number of photos", "Labels", and "Share". If sticking strictly to code reusability without adding resource files, fallback to hardcoded strings where resources do not exist.

## 7. Quality Standards (Mandatory)
- [x] **100% Test Coverage:** Write comprehensive unit and UI tests for the new `QuestionnaireListScreen` and any related navigation/state management logic.
- [x] **100% Documentation Coverage:** Ensure all new functions, classes, and properties include complete KDoc comments to strictly meet the repository's documentation requirements.
