ChartCam
========

![Doc Coverage](https://img.shields.io/badge/Doc%20Coverage-100.0%25-brightgreen)
![Test Coverage](https://img.shields.io/badge/Test%20Coverage-100.0%25-brightgreen)
[![Coverage Verification](https://github.com/SamuelMarks/chart-cam/actions/workflows/coverage.yml/badge.svg)](https://github.com/SamuelMarks/chart-cam/actions/workflows/coverage.yml)
[![ChartCam CI/CD](https://github.com/SamuelMarks/chart-cam/actions/workflows/deploy.yml/badge.svg)](https://github.com/SamuelMarks/chart-cam/actions/workflows/deploy.yml)

ChartCam is an enterprise-grade Kotlin Multiplatform (KMP) application engineered for modern healthcare platforms. It provides a secure, efficient, and compliant solution for capturing, managing, and triaging patient clinical photography. Architected specifically for practitioners operating in fast-paced, high-friction environments, ChartCam facilitates rapid clinical documentation while maintaining strict adherence to global data privacy regulations (e.g., HIPAA, GDPR, PIPEDA).

---

## 📖 Documentation Directory

To maintain focus and readability, our documentation is logically partitioned into specific domains:

* **[Usage Guide (`USAGE.md`)](USAGE.md)**: A comprehensive guide on operating the application, detailing our "Snap-First" philosophy and optimized practitioner workflows.
* **[Release Guide (`HOW_TO_RELEASE.md`)](HOW_TO_RELEASE.md)**: Standard Operating Procedures (SOPs) for building, signing, and deploying the application to the **Google Play Store** and **Apple App Store**.
* **[Navigation Architecture (`docs/NAVIGATION.md`)](docs/NAVIGATION.md)**: Technical breakdown of the application's routing logic, state management, and decision trees.

---

## 🏥 Healthcare Interoperability & FHIR Standards

ChartCam takes clinical data interoperability and regulatory compliance as foundational pillars. At its core, the application is designed around the **Fast Healthcare Interoperability Resources (FHIR)** standard (HL7 FHIR R4), ensuring that all captured data is structured, standardized, and primed for seamless integration into any modern Electronic Health Record (EHR) system.

### What is Modelled?

*   **Patient & Encounter Resources:** Every clinical interaction is strictly modeled using FHIR `Patient` and `Encounter` resources. This guarantees an unambiguous, standardized linkage between the subject of care and the specific clinical event.
*   **DocumentReference & Media:** Captured clinical photography is never treated as a loosely managed raw file. Instead, images are immediately encapsulated within FHIR `DocumentReference` or `Media` resources. This rich metadata wrapper includes the author (practitioner), capture datetime, anatomical site (codified via SNOMED CT or LOINC), and capturing device information.
*   **Questionnaire & QuestionnaireResponse:** To support dynamic, compliance-driven clinical intake forms and triage assessments, ChartCam heavily leverages `Questionnaire` and `QuestionnaireResponse` resources. This enables healthcare organizations to deploy custom, structured data collection instruments (e.g., informed consent forms, specialized wound assessment scales) that render natively within the KMP application layer.
*   **Security & Provenance:** Patient consent and data provenance are rigorously managed. Clinical media is associated with `Consent` and `Provenance` resources to maintain an immutable, auditable trail of data capture and access, aligning with stringent HIPAA and security audit requirements.

By natively speaking FHIR, ChartCam eliminates the traditional need for fragile, complex middleware transformations, empowering organizations to directly route high-fidelity clinical media into a patient's longitudinal health record.

---

## ✨ Features & Tech Stack

ChartCam leverages modern Android and KMP best practices to deliver a consistent, high-performance experience across all platforms:

* **UI**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) (100% shared UI layer).
* **Architecture**: Unidirectional Data Flow (UDF) and MVVM based on Clean Architecture principles.
* **FHIR Engine**: Native integration of FHIR models for interoperable clinical data rendering and transmission.
* **Navigation**: Type-safe Compose Navigation.
* **Local Data**: [SQLDelight](https://cashapp.github.io/sqldelight/) for robust, type-safe local relational data storage.
* **Security**: Platform-specific encrypted storage modules (EncryptedSharedPreferences on Android & Keychain on iOS).
* **Hardware Interop**: Native camera and sensor integrations via `expect/actual` paradigms (CameraX for Android, AVFoundation for iOS).
* **CI/CD**: Fastlane configured for automated testing, signing, and continuous delivery.

### Feature Availability Matrix

While the **Business Logic (FHIR, Auth, ViewModels)** and **UI (Compose)** are 100% shared, hardware-specific capabilities are implemented natively via `expect/actual` bindings to ensure maximum performance and reliability.

| Feature               |     🤖 Android     |      🍎 iOS      |  🖥️ Desktop (JVM)  | 🌐 Web (JS/Wasm) | Source Location         |
|:----------------------|:------------------:|:----------------:|:-------------------:|:----------------:|:------------------------|
| **UI Rendering**      |         ✅          |        ✅         |          ✅          |        ✅         | `commonMain/ui`         |
| **Navigation**        |         ✅          |        ✅         |          ✅          |        ✅         | `commonMain/navigation` |
| **Local Auth**        |         ✅          |        ✅         |          ✅          |        ✅         | `commonMain/repository` |
| **Secure Storage**    | ✅ (EncryptedPrefs) |   ✅ (Keychain)   |  ✅ (AES EncryptedPrefs) |   ✅ (AES Crypto-JS)    | `platform/.../storage`  |
| **Database (SQL)**    | ✅ (AndroidDriver)  | ✅ (NativeDriver) |      ✅ (JDBC)       | ✅ (WebWorkerDriver)| `platform/.../database` |
| **Camera Preview**    |    ✅ (CameraX)     | ✅ (AVFoundation) |  ✅ (Sarxos Webcam)  |  ✅ (HTMLVideo)  | `platform/.../camera`   |
| **Photo Capture**     |         ✅          |        ✅         |  ✅ (Sarxos Webcam)   |  ✅ (HTMLCanvas) | `platform/.../camera`   |
| **Sensors (Leveler)** | ✅ (SensorManager)  |  ✅ (CoreMotion)  |    ⚠️ (Fixed 0°)    |  ⚠️ (Fixed 0°)   | `platform/.../sensors`  |
| **File I/O**          |         ✅          |        ✅         |          ✅          |   ✅ (Memory Cache)   | `platform/.../files`    |

**Legend:**
* ✅ **Fully Supported**: Core logic and native platform implementation provided.
* ⚠️ **Partial / Fallback**: UI renders appropriately, but hardware logic is stubbed out (no crash, simulated data).
* ❌ **Not Supported**: Feature is out of scope for the current target release.

> **Note on Web & Desktop:** Full web support for device orientation sensors requires implementing `actual` bindings using the HTML5 DeviceOrientation API. Currently, Web supports HTML5 Camera Capture, while the JVM Desktop target leverages the Sarxos Webcam library for robust external camera support.

---

## 🏗️ Project Structure

* **/chartCam**: The core KMP module encompassing shared logic (Android, iOS, Desktop, Web) and Android-specific implementations.
    * `commonMain`: Shared business logic, UI (Compose), FHIR models, and ViewModels.
    * `androidMain`: Android-specific platform bindings (CameraX, Sensors, Keystore).
    * `iosMain`: iOS-specific platform bindings (AVFoundation, CoreMotion, Keychain).
    * `jvmMain` / `wasmJsMain`: Desktop and Web hardware stubs and fallbacks.
* **/androidApp**: A thin execution wrapper providing the `MainActivity` and `AndroidManifest.xml` for the Android application context.
* **/iosApp**: The iOS entry point (Xcode project) that integrates and consumes the shared KMP framework.
* **/fastlane**: CI/CD automation configuration for testing, code signing, and store deployments.
* **/.github/workflows**: GitHub Actions pipeline definitions for continuous integration.
* **/docs**: Deeper architectural documentation (e.g., `NAVIGATION.md`).

---

## 🚀 Getting Started

### Prerequisites

To build and run this project locally, ensure your development environment is provisioned with:
1. **[JDK 17+](https://adoptium.net/)**
2. **[Android Studio (latest stable)](https://developer.android.com/studio)** (for Android and shared KMP development)
3. **[Xcode](https://developer.apple.com/xcode/)** (for iOS compilation, macOS required)
4. **[Ruby & Bundler](https://bundler.io/)** (required for Fastlane CI/CD automation)

### 1. Install Ruby Dependencies

We utilize [Fastlane](https://fastlane.tools/) to automate our test suites and store releases. Initialize the environment by running:

```shell
bundle install
```

### 2. Fastlane Configuration

The `fastlane/Fastfile` houses deployment lanes for both Android and iOS. For detailed configuration steps required to execute a production release, please refer to [HOW_TO_RELEASE.md](HOW_TO_RELEASE.md).

---

## 💻 Development & Building

### Build and Run Android

Launch the application directly from Android Studio by selecting the `androidApp` or `chartCam` run configuration, or compile via the CLI:

```shell
./gradlew :chartCam:assembleDebug
```

### Build and Run iOS

Open `./iosApp/iosApp.xcodeproj` in Xcode and execute the **Run** command (Cmd+R).
*Note: Ensure the Kotlin Multiplatform plugin in Android Studio is fully synced to generate the required iOS framework before compiling in Xcode.*

### Build and Run Desktop

To validate the Compose Multiplatform UI across desktop environments:

```shell
./gradlew :chartCam:run
```

---

## 🧪 Quality Assurance & Testing

ChartCam mandates rigorous testing protocols. There are two primary methodologies for executing test suites: via **Fastlane** (Unified pipeline) or **Gradle** (Targeted).

### 1. Via Fastlane (Recommended)

This approach executes unit tests across all available platform targets in a single, unified command, mirroring our CI/CD pipeline environment.

```bash
# Execute all unit tests (Common, Android, iOS)
bundle exec fastlane test_all

# Execute Android-specific tests
bundle exec fastlane android test

# Execute iOS-specific tests (macOS environment required)
bundle exec fastlane ios test
```

### 2. Via Gradle Wrapper

For targeted debugging, execute tests against specific platforms directly via Gradle:

| Target           | Command                                       | Description                                          |
|:-----------------|:----------------------------------------------|:-----------------------------------------------------|
| **Common Logic** | `./gradlew test`                              | Validates shared business logic (runs on JVM host).  |
| **Android**      | `./gradlew :chartCam:testDebugUnitTest`     | Executes Android-specific instrumentation and unit tests. |
| **iOS**          | `./gradlew :chartCam:iosSimulatorArm64Test` | Executes Kotlin/Native tests on the iOS Simulator.   |
| **Desktop**      | `./gradlew :chartCam:jvmTest`               | Executes Desktop-specific logic tests.               |

**Test Reports Location:**
Upon completion, comprehensive HTML test coverage and result reports are generated at:
* `chartCam/build/reports/tests/`

---
> **Compliance Note:** ChartCam enforces 100% Documentation and Test Coverage standards. Execute `./gradlew check` to verify compliance prior to submitting any Pull Requests.
