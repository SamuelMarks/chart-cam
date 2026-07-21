.PHONY: clean build test lint build_release_android build_release_ios build_release_jvm build_release_js build_release_wasm run_android run_ios run_jvm

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
	@echo "Creating iOS archive..."
	cd iosApp && xcodebuild -scheme iosApp -configuration Release -destination 'generic/platform=iOS' CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO archive -archivePath ../build/ios/ChartCam.xcarchive
	@echo "================================================================="
	@echo "Archive successfully generated at:"
	@echo "  build/ios/ChartCam.xcarchive"
	@echo ""
	@echo "If you need an unsigned .ipa, you can manually zip the .app:"
	@echo "  mkdir -p build/ios/Payload"
	@echo "  cp -R build/ios/ChartCam.xcarchive/Products/Applications/ChartCam.app build/ios/Payload/"
	@echo "  cd build/ios && zip -r ChartCam-unsigned.ipa Payload"
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
	@xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' build
	@echo "Installing and launching..."
	@APP_PATH="$$(xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -destination 'generic/platform=iOS Simulator' -showBuildSettings | grep -w TARGET_BUILD_DIR | awk '{print $$3}')/ChartCam.app" && \
	xcrun simctl install booted "$$APP_PATH" && \
	xcrun simctl launch booted "io.healthplatform.chartcam.ChartCam"

run_jvm:
	./gradlew --console=plain :chartCam:run
