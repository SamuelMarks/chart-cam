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
if /i "%~1"=="run_android" goto run_android
if /i "%~1"=="run_ios" goto run_ios
if /i "%~1"=="run_jvm" goto run_jvm

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
echo   run_android
echo   run_ios
echo   run_jvm
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

:run_android
adb get-state 1>nul 2>nul
if not errorlevel 1 goto android_install
echo Starting emulator...
for /f "tokens=*" %%i in ('emulator -list-avds') do (
    set AVD_NAME=%%i
    goto start_emulator
)
:start_emulator
start /b emulator -avd %AVD_NAME%
echo Waiting for emulator to boot...
adb wait-for-device
:wait_boot
for /f "tokens=*" %%i in ('adb shell getprop sys.boot_completed') do set BOOT_COMPLETED=%%i
if not "%BOOT_COMPLETED%"=="1" (
    timeout /t 1 /nobreak >nul
    goto wait_boot
)
:android_install
call gradlew.bat :androidApp:installDebug
adb shell am start -n "io.healthplatform.chartcam/io.healthplatform.chartcam.android.MainActivity" -a android.intent.action.MAIN -c android.intent.category.LAUNCHER
goto end

:run_ios
echo Running iOS from Windows is not supported.
goto end

:run_jvm
call gradlew.bat :chartCam:run
goto end

:end
endlocal
