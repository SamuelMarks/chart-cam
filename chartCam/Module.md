# ChartCam Module

ChartCam is a Kotlin Multiplatform (KMP) module that encompasses the shared business logic, UI layer, and platform-specific hardware integrations for the ChartCam application.

## Architecture

The module is structured following Clean Architecture principles and Unidirectional Data Flow (UDF). It utilizes Compose Multiplatform for a 100% shared UI layer and manages state via viewmodels.
Data is persisted locally using SQLDelight and encrypted storage wrappers. Interactions with hardware (camera, sensors) are abstracted using KMP's `expect`/`actual` paradigm.

## Data Flow

Data flows from the local secure storage or the FHIR-based backend repository into the ViewModels, which expose immutable UI states. The Compose UI layer observes these states and issues user events back to the ViewModels, triggering state updates or external actions like capturing photos or syncing data.

## Platform Bindings

- **Android (`androidMain`)**: Implements CameraX for photo capture, standard SensorManager for device leveling, and EncryptedSharedPreferences for secure key storage.
- **iOS (`iosMain`)**: Implements AVFoundation for camera capture, CoreMotion for leveling, and Keychain for secure storage.
- **JVM (`jvmMain`)**: Provides Sarxos Webcam support for desktop, and AES-encrypted properties for storage.
- **Web (`wasmJsMain` / `jsMain`)**: Utilizes HTML5 Video/Canvas for capture and encrypted local storage.
