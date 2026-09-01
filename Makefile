-include .env

.PHONY: clean build test lint build_release_android build_release_ios build_adhoc_ios deploy_ios_to_firebase build_release_jvm build_release_js build_release_wasm run_android run_ios run_jvm

clean:
	./gradlew --console=plain clean

build:
	./gradlew --console=plain assemble

test:
	./gradlew --console=plain allTests

lint:
	./gradlew --console=plain lint

build_release_android:
	./gradlew --console=plain :androidApp:assembleRelease

build_release_ios:
	@if [ -z "$(APPLE_TEAM_ID)" ]; then echo "Error: APPLE_TEAM_ID is not set in .env"; exit 1; fi
	@if [ -z "$(APPLE_PROVISIONING_PROFILE)" ]; then echo "Error: APPLE_PROVISIONING_PROFILE is not set in .env"; exit 1; fi
	@echo "Creating iOS archive..."
	cd iosApp && PATH="/usr/bin:$$PATH" xcodebuild -scheme iosApp -configuration Release -destination 'generic/platform=iOS' DEVELOPMENT_TEAM="$(APPLE_TEAM_ID)" PROVISIONING_PROFILE_SPECIFIER="$(APPLE_PROVISIONING_PROFILE)" EXPORT_COMPLIANCE_CODE="$(EXPORT_COMPLIANCE_CODE)" archive -archivePath ../build/ios/ChartCam.xcarchive -allowProvisioningUpdates
	@echo "Exporting .ipa for App Store Connect..."
	@mkdir -p build/ios
	@echo '<?xml version="1.0" encoding="UTF-8"?>' > build/ios/exportOptions.plist
	@echo '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">' >> build/ios/exportOptions.plist
	@echo '<plist version="1.0">' >> build/ios/exportOptions.plist
	@echo '<dict>' >> build/ios/exportOptions.plist
	@echo '    <key>method</key>' >> build/ios/exportOptions.plist
	@echo '    <string>app-store-connect</string>' >> build/ios/exportOptions.plist
	@echo '    <key>teamID</key>' >> build/ios/exportOptions.plist
	@echo '    <string>$(APPLE_TEAM_ID)</string>' >> build/ios/exportOptions.plist
	@echo '    <key>uploadSymbols</key>' >> build/ios/exportOptions.plist
	@echo '    <true/>' >> build/ios/exportOptions.plist
	@echo '    <key>signingStyle</key>' >> build/ios/exportOptions.plist
	@echo '    <string>manual</string>' >> build/ios/exportOptions.plist
	@echo '    <key>manageAppVersionAndBuildNumber</key>' >> build/ios/exportOptions.plist
	@echo '    <false/>' >> build/ios/exportOptions.plist
	@echo '    <key>destination</key>' >> build/ios/exportOptions.plist
	@echo '    <string>export</string>' >> build/ios/exportOptions.plist
	@echo '    <key>provisioningProfiles</key>' >> build/ios/exportOptions.plist
	@echo '    <dict>' >> build/ios/exportOptions.plist
	@echo '        <key>io.healthplatform.chartcam</key>' >> build/ios/exportOptions.plist
	@echo '        <string>$(APPLE_PROVISIONING_PROFILE)</string>' >> build/ios/exportOptions.plist
	@echo '    </dict>' >> build/ios/exportOptions.plist
	@echo '</dict>' >> build/ios/exportOptions.plist
	@echo '</plist>' >> build/ios/exportOptions.plist
	cd iosApp && PATH="/usr/bin:$$PATH" xcodebuild -exportArchive -archivePath ../build/ios/ChartCam.xcarchive -exportOptionsPlist ../build/ios/exportOptions.plist -exportPath ../build/ios/export DEVELOPMENT_TEAM="$(APPLE_TEAM_ID)"
	@echo "================================================================="
	@echo "IPA successfully generated at:"
	@echo "  build/ios/export/ChartCam.ipa"
	@echo "You can upload this file using the Apple Transporter app."
	@echo "================================================================="

build_adhoc_ios:
	@if [ -z "$(APPLE_TEAM_ID)" ]; then echo "Error: APPLE_TEAM_ID is not set in .env"; exit 1; fi
	@if [ -z "$(APPLE_PROVISIONING_PROFILE_ADHOC)" ]; then echo "Error: APPLE_PROVISIONING_PROFILE_ADHOC is not set in .env (needed for Firebase App Distro)"; exit 1; fi
	@echo "Creating iOS Ad-Hoc archive..."
	cd iosApp && PATH="/usr/bin:$$PATH" xcodebuild -scheme iosApp -configuration Release -destination 'generic/platform=iOS' DEVELOPMENT_TEAM="$(APPLE_TEAM_ID)" PROVISIONING_PROFILE_SPECIFIER="$(APPLE_PROVISIONING_PROFILE_ADHOC)" EXPORT_COMPLIANCE_CODE="$(EXPORT_COMPLIANCE_CODE)" archive -archivePath ../build/ios/ChartCam_AdHoc.xcarchive -allowProvisioningUpdates
	@echo "Exporting .ipa for Ad-Hoc Distribution..."
	@mkdir -p build/ios/export_adhoc
	@echo '<?xml version="1.0" encoding="UTF-8"?>' > build/ios/exportOptions_adhoc.plist
	@echo '<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">' >> build/ios/exportOptions_adhoc.plist
	@echo '<plist version="1.0">' >> build/ios/exportOptions_adhoc.plist
	@echo '<dict>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <key>method</key>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <string>ad-hoc</string>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <key>teamID</key>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <string>$(APPLE_TEAM_ID)</string>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <key>signingStyle</key>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <string>manual</string>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <key>manageAppVersionAndBuildNumber</key>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <false/>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <key>destination</key>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <string>export</string>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <key>provisioningProfiles</key>' >> build/ios/exportOptions_adhoc.plist
	@echo '    <dict>' >> build/ios/exportOptions_adhoc.plist
	@echo '        <key>io.healthplatform.chartcam</key>' >> build/ios/exportOptions_adhoc.plist
	@echo '        <string>$(APPLE_PROVISIONING_PROFILE_ADHOC)</string>' >> build/ios/exportOptions_adhoc.plist
	@echo '    </dict>' >> build/ios/exportOptions_adhoc.plist
	@echo '</dict>' >> build/ios/exportOptions_adhoc.plist
	@echo '</plist>' >> build/ios/exportOptions_adhoc.plist
	cd iosApp && PATH="/usr/bin:$$PATH" xcodebuild -exportArchive -archivePath ../build/ios/ChartCam_AdHoc.xcarchive -exportOptionsPlist ../build/ios/exportOptions_adhoc.plist -exportPath ../build/ios/export_adhoc DEVELOPMENT_TEAM="$(APPLE_TEAM_ID)"
	@echo "================================================================="
	@echo "Ad-Hoc IPA successfully generated at:"
	@echo "  build/ios/export_adhoc/ChartCam.ipa"
	@echo "================================================================="

deploy_ios_to_firebase: build_adhoc_ios
	@if [ -z "$(FIREBASE_IOS_APP_ID)" ]; then echo "Error: FIREBASE_IOS_APP_ID is not set in .env"; exit 1; fi
	@echo "Uploading to Firebase App Distribution..."
	npx --yes firebase-tools appdistribution:distribute build/ios/export_adhoc/ChartCam.ipa \
		--app $(FIREBASE_IOS_APP_ID) \
		--release-notes "Stopgap release" \
		--testers "$(FIREBASE_TESTERS)"
	@echo "================================================================="
	@echo "Successfully uploaded to Firebase App Distribution!"
	@echo "================================================================="

build_release_jvm:
	./gradlew --console=plain :chartCam:packageReleaseDistributionForCurrentOS

build_release_js:
	./gradlew --console=plain :chartCam:jsBrowserDistribution

build_release_wasm:
	./gradlew --console=plain :chartCam:wasmJsBrowserDistribution

run_android:
	@if ! adb get-state 1>/dev/null 2>&1; then \
		echo "Starting emulator..."; \
		emulator -avd $$(emulator -list-avds | head -n 1) > /dev/null 2>&1 & \
		echo "Waiting for emulator to boot..."; \
		adb wait-for-device; \
		while [ "$$(adb shell getprop sys.boot_completed | tr -d '\r')" != "1" ]; do sleep 1; done; \
	fi
	./gradlew --console=plain :androidApp:installDebug
	adb shell am start -n "io.healthplatform.chartcam/io.healthplatform.chartcam.android.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER

run_ios:
	@echo "Starting iOS Simulator..."
	@open -a Simulator
	@echo "Building iOS app..."
	@PATH="/usr/bin:$$PATH" xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build
	@echo "Installing and launching..."
	@APP_PATH="$$(PATH="/usr/bin:$$PATH" xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -showBuildSettings | grep -w TARGET_BUILD_DIR | awk '{print $$3}')/ChartCam.app" && \
	xcrun simctl install booted "$$APP_PATH" && \
	xcrun simctl launch booted "io.healthplatform.chartcam.ChartCam"

run_jvm:
	./gradlew --console=plain :chartCam:run
