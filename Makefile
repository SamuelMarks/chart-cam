.PHONY: build test lint build_release_android build_release_ios build_release_jvm build_release_js build_release_wasm

build:
	./gradlew assemble

test:
	./gradlew allTests

lint:
	./gradlew lint

build_release_android:
	./gradlew :androidApp:assembleRelease

build_release_ios:
	cd iosApp && xcodebuild -scheme iosApp -configuration Release CODE_SIGN_IDENTITY="" CODE_SIGNING_REQUIRED=NO CODE_SIGNING_ALLOWED=NO build

build_release_jvm:
	./gradlew :chartCam:packageReleaseDistributionForCurrentOS

build_release_js:
	./gradlew :chartCam:jsBrowserDistribution

build_release_wasm:
	./gradlew :chartCam:wasmJsBrowserDistribution
