# ChartCam Release Management Guide

This document defines the standardized operational procedures for deploying the ChartCam application to production environments, specifically targeting the **Google Play Store** (Android) and the **Apple App Store** (iOS). To ensure reproducibility, minimize human error, and streamline continuous delivery, ChartCam relies on [Fastlane](https://fastlane.tools/) for automated build, code signing, and release pipelines.

---

## 🤖 Android Deployment (Google Play Console)

The Android release process necessitates compiling a signed Android App Bundle (AAB), managing versioning, and deploying the artifact through the Google Play Console's defined testing and production tracks.

### Prerequisites & Security

1.  **Production Keystore**: Secure access to the authoritative production keystore file (`chartcam-release.keystore`). This artifact is highly sensitive and must be managed according to internal security policies.
2.  **Keystore Configuration**: The build environment (local or CI runner) must securely inject the necessary keystore credentials. This is typically achieved via `local.properties` or secure environment variables.
    ```properties
    # local.properties (Excluded from Version Control)
    RELEASE_STORE_FILE=chartcam-release.keystore
    RELEASE_STORE_PASSWORD=<SECURE_STORE_PASSWORD>
    RELEASE_KEY_ALIAS=<SECURE_KEY_ALIAS>
    RELEASE_KEY_PASSWORD=<SECURE_KEY_PASSWORD>
    ```
3.  **Google Play Service Account**: A valid JSON service account key (`api-key.json`) provisioned with appropriate permissions in the Google Play Developer Console. This file must reside at `fastlane/api-key.json` or be securely injected during CI execution.

### Manual Artifact Generation (Fallback)

In scenarios requiring manual artifact inspection or offline compilation, the release AAB can be generated directly via Gradle:

```bash
./gradlew :composeApp:bundleRelease
```
Upon successful execution, the optimized App Bundle will be placed in `composeApp/build/outputs/bundle/release/composeApp-release.aab`.

### Automated Deployment Pipeline (Fastlane)

Fastlane is the primary, supported mechanism for all ChartCam deployments, handling the complete lifecycle from compilation to store submission.

#### 1. Internal Testing / QA Verification

Deploy an immediate release to the Internal Testing track to distribute the build to designated QA personnel and internal stakeholders:

```bash
bundle exec fastlane android deploy_internal
```

#### 2. Beta / Open Testing Promotion

Following successful QA verification, promote the application to the Beta testing track for broader audience validation:

```bash
bundle exec fastlane android deploy_beta
```

#### 3. Production Release

Execute the final deployment to the Production track (typically post-Beta validation). This action makes the application publicly available:

```bash
bundle exec fastlane android deploy_production
```

> **Versioning Note:** The Fastlane pipeline is configured to automatically query the Google Play API and increment the `versionCode` relative to the latest published artifact. Developers are responsible for manually updating the semantic `versionName` within `composeApp/build.gradle.kts` prior to initiating a release lane.

---

## 🍎 iOS Deployment (App Store Connect & TestFlight)

The iOS deployment pipeline involves managing provisioning profiles, executing cryptographic code signing, compiling the iOS App Store Package (IPA), and interfacing with Apple's App Store Connect API.

### Prerequisites & Environment Setup

1.  **macOS Host**: Compilation and signing of iOS artifacts mandates a macOS environment running the latest stable release of Xcode.
2.  **Apple Developer Program Membership**: Active enrollment and Administrative or App Manager roles within the organizational Apple Developer account.
3.  **App-Specific Passwords**: Generation of an App-Specific Password via appleid.apple.com to authenticate Fastlane tools (e.g., `pilot`, `deliver`) against Apple's infrastructure.
4.  **Fastlane Match Configuration**: ChartCam utilizes `match` to enforce deterministic, repository-driven management of code signing certificates and provisioning profiles across the development team and CI runners.

### Cryptographic Code Signing Initialization

Prior to building, the environment must synchronize the correct production certificates and profiles from the secure, encrypted repository:

```bash
bundle exec fastlane match appstore
```
*(Execution will prompt for the shared match repository decryption passphrase)*

### Manual Artifact Generation (Xcode Fallback)

If manual compilation is necessary:
1. Open the project workspace: `./iosApp/iosApp.xcodeproj`.
2. Configure the active scheme destination to **Any iOS Device (arm64)**.
3. Navigate to `Product > Archive`.
4. Upon successful archiving, the Organizer window will launch. Select **Distribute App** and proceed through the native App Store Connect upload flow.

### Automated Deployment Pipeline (Fastlane)

#### 1. TestFlight Deployment (Internal/External Beta)

Compile, sign, and upload the artifact to TestFlight for rigorous beta testing:

```bash
bundle exec fastlane ios deploy_testflight
```
This automated lane performs the following operations:
- Synchronizes provisioning profiles and certificates via `match`.
- Automatically increments the CFBundleVersion (build number).
- Compiles the application into a distribution-ready `.ipa` utilizing `gym`.
- Uploads the artifact and release notes to TestFlight via `pilot`.

#### 2. App Store Submission (Production)

Initiate the final submission process for Apple App Store Review:

```bash
bundle exec fastlane ios deploy_appstore
```
This lane performs the following operations:
- Verifies cryptographic signing integrity for App Store distribution.
- Uploads the compiled artifact and associated metadata (screenshots, descriptions) to App Store Connect.
- Submits the release candidate to the App Store Review queue (subject to Fastfile configuration specifics).

### Post-Release Lifecycle Management

Following a successful production release on either platform, the following administrative tasks must be executed:

1.  **Version Control Tagging**: Create an immutable Git tag corresponding to the released version to ensure historical traceability:
    ```bash
    git tag -a v1.0.0 -m "Production Release: Version 1.0.0"
    git push origin v1.0.0
    ```
2.  **Version String Bump**: Increment the semantic version identifiers (`versionName` in `composeApp/build.gradle.kts` and `CFBundleShortVersionString` in `iosApp/Info.plist`) to prepare the codebase for the subsequent development iteration.
