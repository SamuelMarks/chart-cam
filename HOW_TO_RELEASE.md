# 🚀 ChartCam Ultimate Release Guide

This document is the authoritative, exhaustive guide for releasing the ChartCam application to the **Google Play Store** (Android) and the **Apple App Store** (iOS).

Whether you are configuring the project for the first time, automating the pipeline via CI/CD, or manually submitting an update from your IDE, follow these phases strictly to ensure a flawless deployment.

---

## 🛑 Phase 0: Pre-Flight Checklist

Before generating any artifacts, ensure the codebase is stable and clean:
1. **Ensure a clean working tree:** You should be on the `master` or `release` branch with no uncommitted changes. (`git status`)
2. **Run all tests:** Execute the unit and UI tests to prevent regressions.
   ```bash
   ./gradlew test_all  # Or run the fastlane test lane: bundle exec fastlane test_all
   ```
3. **Verify Configuration:** Ensure `RELEASE_STORE_FILE` and API keys are properly configured in `local.properties` (Android) and `match` is accessible (iOS).

---

## 🏗 Phase 1: First-Time Setup (One-Off Tasks)

*If ChartCam is already published and you are just updating it, skip to Phase 2.*

### 1.1 Google Play Store Initialization
1.  **Create App:** Log into the [Google Play Console](https://play.google.com/console), click **Create app** (ChartCam, App, Free).
2.  **Store Presence:** Fill out the Main Store Listing (Description, Icon, Screenshots, Feature Graphic).
3.  **App Content:** Complete all questionnaires: Privacy Policy, Data Safety, Content Rating, and Target Audience.
4.  **Generate Keystore:** Create your production signing key (Do NOT lose this, and do NOT commit it to git):
    ```bash
    keytool -genkey -v -keystore chartcam-release.keystore -alias chartcam -keyalg RSA -keysize 2048 -validity 10000
    ```
5.  **Service Account API Key:** Go to Setup > API Access. Create a Google Cloud Service Account, grant it Admin/Release permissions, download the JSON key, and save it as `fastlane/api-key.json`.

### 1.2 Apple App Store Initialization
1.  **App ID Setup:** Log into the [Apple Developer Portal](https://developer.apple.com/account/). Under **Identifiers**, register `io.healthplatform.chartcam` (Explicit App ID).
2.  **App Store Connect:** Log into [App Store Connect](https://appstoreconnect.apple.com/). Click **+ New App**. Select the Bundle ID, name it ChartCam, and fill in initial metadata.
3.  **Fastlane Match:** Initialize your encrypted certificate repository.
    ```bash
    bundle exec fastlane match init
    bundle exec fastlane match appstore
    ```

---

## 🏷 Phase 2: Version Bumping

Both App Stores will reject binaries if the version numbers are not strictly incremented from the previous release.

### 2.1 Android (Google Play)
1. Open `chartCam/build.gradle.kts`.
2. Increment `versionCode` (Integer used internally by Google Play. Must be +1 of the last release).
3. Update `versionName` (The semantic public version, e.g., `"1.2.0"`).
4. Sync the Gradle project.

### 2.2 iOS (App Store)
1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Select the `iosApp` target in the left navigator.
3. Under the **General** tab -> **Identity**:
   * Update **Version** to match the Android `versionName` (e.g., `1.2.0`).
   * Update **Build** to a unique number (e.g., `42`).
   
*Pro-tip: To skip Apple's manual export compliance prompt every time you upload, ensure `ITSAppUsesNonExemptEncryption` is set to `NO` in your `Info.plist` (unless ChartCam uses proprietary encryption).*

---

## 🤖 Phase 3: Android Build & Upload

Choose your preferred approach: CLI/Automated (Recommended) or IDE (Manual).

### Approach A: CLI & Fastlane (Automated / CI)
*This is the standard approach for our GitHub Actions pipeline.*
1. Ensure `local.properties` contains your keystore secrets:
   ```properties
   RELEASE_STORE_FILE=/path/to/chartcam-release.keystore
   RELEASE_STORE_PASSWORD=***
   RELEASE_KEY_ALIAS=chartcam
   RELEASE_KEY_PASSWORD=***
   ```
2. Build and upload directly to the Internal Testing track:
   ```bash
   bundle exec fastlane android deploy_internal
   ```
3. Check the Fastlane output for a successful upload message.

### Approach B: Android Studio (Manual GUI)
1. Open Android Studio.
2. Go to **Build > Generate Signed Bundle / APK...**
3. Select **Android App Bundle** (AAB) > **Next**.
4. Provide the path to `chartcam-release.keystore`, alias, and passwords. Click **Next**.
5. Select the **release** build variant and click **Finish**.
6. Wait for the compilation. Locate `chartCam-release.aab` in `chartCam/build/outputs/bundle/release/`.
7. Log into Google Play Console > **Internal testing** > **Create new release**.
8. Drag and drop the `.aab` file, write release notes, and click **Save** then **Roll out**.

---

## 🍎 Phase 4: iOS Build & Upload

Choose your preferred approach: CLI/Automated (Recommended) or IDE (Manual).

### Approach A: CLI & Fastlane (Automated / CI)
1. Ensure you have Apple credentials set up (either `FASTLANE_APPLE_APPLICATION_SPECIFIC_PASSWORD` environment variable or an App Store Connect API Key in the Fastfile).
2. Fetch the latest certificates and provisioning profiles:
   ```bash
   bundle exec fastlane match appstore
   ```
3. Build the IPA and upload it to TestFlight:
   ```bash
   bundle exec fastlane ios deploy_testflight
   ```
4. Fastlane will automatically compile via `gym` and upload via `pilot`.

### Approach B: Xcode (Manual GUI)
1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Select the `iosApp` target > **Signing & Capabilities**. Check **Automatically manage signing** and select your Team.
3. In the device target dropdown (top center), select **Any iOS Device (arm64)**. *(Important: Archiving will fail if a Simulator is selected).*
4. In the top menu, go to **Product > Archive**.
5. Once compiled, the **Organizer** window will open. Select your archive and click **Distribute App**.
6. Select **TestFlight & App Store** > **Upload**.
7. Keep default distribution options (Manage Version, Strip Swift Symbols).
8. Choose **Automatically manage signing**.
9. Click **Upload** and wait for the green success checkmark.

---

## 🌍 Phase 5: Store Submission & Release Management

Uploading the binary does not immediately publish the app to users. You must promote it.

### Google Play Store
1. Go to **Releases overview**.
2. Promote the build from **Internal Testing** to **Closed Testing (Beta)** or directly to **Production**.
3. Review the rollout screen, address any missing declarations, and click **Start Rollout to Production**.
4. The app will enter "In Review" status.

### Apple App Store
1. Go to App Store Connect > **My Apps** > **ChartCam**.
2. Navigate to the **TestFlight** tab. Wait for the build to finish "Processing".
3. Once processed, assign groups to begin Beta testing.
4. To release to the public, go to the **App Store** tab.
5. Create a new version (e.g., 1.2.0).
6. Scroll to the **Build** section, click `+`, and select the binary you uploaded.
7. Fill out the "What's New" release notes.
8. Click **Add for Review**.
9. Once approved by Apple, you can manually release it or let it release automatically.

---

## 🧹 Phase 6: Post-Release Cleanup

Always tag the repository at the exact state it was published. This is crucial for debugging production crashes.

```bash
# Ensure you are on the commit that was just released
git tag -a v1.2.0 -m "Production Release: Version 1.2.0"
git push origin v1.2.0
```

**Final Step:** Monitor Firebase Crashlytics (or your chosen analytics platform) heavily for the first 48 hours after a rollout to catch any critical production-only regressions.