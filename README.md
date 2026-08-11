ChartCam
========

[![License](https://img.shields.io/badge/license-Apache--2.0%20OR%20MIT-blue.svg)](https://opensource.org/licenses/Apache-2.0)
![Doc Coverage](https://img.shields.io/badge/Doc%20Coverage-100.0%25-brightgreen)
![Test Coverage](https://img.shields.io/badge/Test%20Coverage-100.0%25-brightgreen)
[![Coverage Verification](https://github.com/SamuelMarks/chart-cam/actions/workflows/coverage.yml/badge.svg)](https://github.com/SamuelMarks/chart-cam/actions/workflows/coverage.yml)
[![ChartCam CI/CD](https://github.com/SamuelMarks/chart-cam/actions/workflows/deploy.yml/badge.svg)](https://github.com/SamuelMarks/chart-cam/actions/workflows/deploy.yml)

> Open-source, decentralized, and FHIR-native. Built for Harvard clinicians to rapidly capture encrypted clinical media and run custom medical studies on the go.

ChartCam is a fully open-source, decentralized clinical data capture and research platform engineered for the modern healthcare ecosystem. Originally developed to support Google-sponsored clinicians at Harvard Medical School’s Mass General Hospital (Mass Eye and Ear Infirmary), ChartCam brings rigorous, privacy-first data collection directly to the point of care.

Far beyond a standard clinical camera, ChartCam is a powerful engine designed to democratize medical research. It empowers clinicians, academics, and researchers to effortlessly build, share, and deploy custom questionnaires to run their own clinical studies. From bespoke triage assessments to specialized patient intake protocols, your team can mobilize dynamic data collection instruments instantly.

**Uncompromising Security & Decentralization**
Patient privacy is built into ChartCam’s foundation. Utilizing a heavily encrypted, decentralized architecture, the application ensures that your clinical data remains sovereign. Patient information is secured on-device using military-grade iOS Keychain encryption—guaranteeing total compliance and rigorous provenance tracking without relying on vulnerable centralized middleware.

**Native FHIR Interoperability**
By speaking natively in the Fast Healthcare Interoperability Resources (HL7 FHIR R4) standard, ChartCam eliminates data silos. Every data point—whether it is a high-fidelity clinical image (`DocumentReference`/`Media`) or custom study data (`QuestionnaireResponse`)—is fully structured, standardized, and primed for immediate integration into any major Electronic Health Record (EHR) system.

**Key Features:**

* **Open-Source Transparency:** Free, extensible, and fully open-source. No vendor lock-in; just community-driven healthcare innovation you can trust and audit.
* **Custom Studies on the Go:** Create and share dynamic, FHIR-based questionnaires to execute bespoke clinical trials, studies, and patient intake workflows.
* **Research-Grade Pedigree:** Engineered for high-friction clinical environments in collaboration with top-tier medical researchers at Mass Eye and Ear.
* **Decentralized & Encrypted:** True data sovereignty. Robust on-device encryption protects sensitive patient data at rest, enforcing strict compliance with global health privacy standards.
* **Rapid "Snap-First" Workflow:** Hardware-accelerated native camera capabilities allow for frictionless, zero-delay clinical photography so you can focus on the patient, not the device.
* **Direct EHR Integration:** FHIR-native architecture means your unstructured media and custom study data are instantly structured for downstream EHR consumption.

ChartCam: The secure, open-source engine for clinical innovation.

**Keywords:** `open-source, fhir, clinical, research, harvard, mgh, decentralized, encrypted, questionnaire, ehr, medical`

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
*   **Questionnaire & QuestionnaireResponse:** To democratize medical research and support dynamic triage, ChartCam heavily leverages `Questionnaire` and `QuestionnaireResponse` resources. This enables clinicians and researchers to effortlessly build, share, and deploy custom data collection instruments (e.g., bespoke clinical trials, specialized patient intake workflows) instantly without relying on centralized middleware.
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

ChartCam is designed as a Kotlin Multiplatform (KMP) project targeting Android, iOS, Desktop (JVM), and Web (JS & Wasm).

* **/chartCam**: The core KMP module encompassing all shared logic and platform-specific implementations.
    * `commonMain`: The unified source of truth containing shared business logic, 100% shared UI (Compose Multiplatform), FHIR models, repository interfaces, and ViewModels.
    * `androidMain`: Android-specific platform bindings (CameraX for camera, SensorManager for leveling, EncryptedSharedPreferences for storage).
    * `iosMain`: iOS-specific platform bindings (AVFoundation for camera, CoreMotion for leveling, Keychain for secure storage).
    * `jvmMain`: Desktop environment hardware stubs and fallback integrations (e.g., Sarxos Webcam).
    * `jsMain` / `wasmJsMain`: Browser implementations leveraging HTML5 canvas for camera, local storage for persistence, and Kotlin/Wasm for high-performance execution.
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

### Makefile Commands (Recommended)

For convenience, a `Makefile` (and an equivalent `make.bat` for Windows) is provided in the root directory with the following standardized commands for building, testing, and packaging across platforms. Windows users should run `make.bat <command>` instead of `make <command>`.

| Command | Description |
|:---|:---|
| `make build` | Assembles all outputs across platforms (without running tests). |
| `make test` | Runs the tests for all targets and creates an aggregated report. |
| `make lint` | Runs Android Lint and other static analysis checks. |
| `make build_release_android` | Assembles the release APK for the Android application. |
| `make build_release_ios` | Builds the Xcode project for iOS release (requires macOS). |
| `make build_release_jvm` | Packages the application for Desktop/JVM distribution on the current OS. |
| `make build_release_js` | Builds the production Web distribution (JavaScript). |
| `make build_release_wasm` | Builds the production Web distribution (WebAssembly). |

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

## License

Licensed under either of

- Apache License, Version 2.0 ([LICENSE-APACHE](LICENSE-APACHE) or <https://www.apache.org/licenses/LICENSE-2.0>)
- MIT license ([LICENSE-MIT](LICENSE-MIT) or <https://opensource.org/licenses/MIT>)

at your option.

### Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in the work by you, as defined in the Apache-2.0 license, shall be
dual licensed as above, without any additional terms or conditions.

