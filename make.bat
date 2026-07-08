@echo off
setlocal

if "%~1"=="" goto help

if /i "%~1"=="build" goto build
if /i "%~1"=="test" goto test
if /i "%~1"=="lint" goto lint
if /i "%~1"=="build_release_android" goto build_release_android
if /i "%~1"=="build_release_ios" goto build_release_ios
if /i "%~1"=="build_release_jvm" goto build_release_jvm
if /i "%~1"=="build_release_js" goto build_release_js
if /i "%~1"=="build_release_wasm" goto build_release_wasm

:help
echo Available targets:
echo   build
echo   test
echo   lint
echo   build_release_android
echo   build_release_ios
echo   build_release_jvm
echo   build_release_js
echo   build_release_wasm
goto end

:build
call gradlew.bat assemble
goto end

:test
call gradlew.bat allTests
goto end

:lint
call gradlew.bat lint
goto end

:build_release_android
call gradlew.bat :androidApp:assembleRelease
goto end

:build_release_ios
echo Building iOS from Windows is not officially supported by Xcode.
echo If you have a custom remote build setup, execute that here.
goto end

:build_release_jvm
call gradlew.bat :chartCam:packageReleaseDistributionForCurrentOS
goto end

:build_release_js
call gradlew.bat :chartCam:jsBrowserDistribution
goto end

:build_release_wasm
call gradlew.bat :chartCam:wasmJsBrowserDistribution
goto end

:end
endlocal
