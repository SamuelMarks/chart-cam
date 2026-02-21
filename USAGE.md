# ChartCam Operational Usage Guide

Welcome to the **ChartCam** operational usage documentation. This guide comprehensively details the application's intended clinical workflows, core features, navigation paradigms, and underlying compliance behaviors for use in professional healthcare settings.

---

## 📸 1. The "Snap-First" Philosophy & Clinical Workflow

ChartCam is architected upon a **"Snap-First"** interaction model, intentionally prioritizing rapid clinical documentation over administrative overhead.

In high-acuity clinical environments, the primary objective is to capture patient photography securely and efficiently. Traditional EHR interfaces often demand complex navigation through patient selection and form-filling before allowing data capture. ChartCam reverses this paradigm. Upon authentication, the practitioner is immediately presented with the **Camera Viewfinder**. This design ensures immediate capture, allowing the administrative task of patient assignment to be deferred to a more appropriate point in the clinical workflow.

### The Standard Practitioner Workflow

1.  **Launch & Authenticate**: Launch the application and securely authenticate via the established Identity Provider (OAuth2/OIDC).
2.  **Immediate Capture**: The application defaults to the Camera viewfinder. The practitioner captures the necessary high-fidelity clinical media without delay.
3.  **Triage & Attribution**: Post-capture, the practitioner reviews the media and securely associates it with either an existing patient record or provisions a new encounter.
4.  **Secure Synchronization**: The application encrypts the media, encapsulates it within standard FHIR resources (`DocumentReference`/`Media`), and seamlessly synchronizes the data with the centralized clinical backend.

---

## 🧭 2. Core Navigation & State Management

ChartCam employs a structured, state-driven navigation architecture to ensure a predictable and secure user experience.

### Authentication (`/login`)
*   **Behavior**: The initial entry point. Users are routed here upon launch or session expiration (driven by JWT token lifecycle).
*   **Action**: Secure credential entry. Successful authentication transitions the application state and automatically routes the user to the Capture interface (`/capture`).

### Clinical Data Capture (`/capture`)
*   **Behavior**: The default, authenticated state. Optimized for immediate hardware access.
*   **Features**:
    *   **High-Fidelity Viewfinder**: Real-time, low-latency camera feed leveraging native hardware APIs.
    *   **Clinical Leveler**: Integration with hardware accelerometers and gyroscopes to ensure orthogonal, standardized clinical photography.
    *   **Secure Capture Pipeline**: Media is captured and immediately written to an encrypted, ephemeral cache.
*   **Navigation Actions**:
    *   **Proceed to Triage**: Upon capturing one or more images, the practitioner advances to the Triage interface.
    *   **Cancel/Back**: 
        *   If the ephemeral cache is empty, the user navigates to the Administrative Dashboard (`/patient_list`).
        *   If the cache contains unsaved media, a mandatory confirmation dialogue ensures intentional data discard, preventing accidental data loss.

### Media Triage & Attribution (`/triage`)
*   **Behavior**: The critical bridge connecting ephemeral, unassigned media to a persistent Patient and Encounter record (modeled as FHIR resources).
*   **Features**:
    *   **Media Review**: A secure gallery for reviewing captured images before final commitment.
    *   **Patient Contextualization**: Powerful search capabilities to locate existing patient records or provision new ones.
*   **Navigation Actions**:
    *   **Attribution**: Selecting a patient definitively links the encrypted media to the patient's Encounter, triggering secure synchronization.
    *   **Provisioning**: Creating a new patient provisions the necessary baseline FHIR `Patient` resource before linking the media.

### Administrative Dashboard & History (`/patient_list`)
*   **Behavior**: The centralized hub for managing clinical documentation and reviewing historical patient data.
*   **Features**:
    *   **Patient Roster**: Access to assigned or clinic-wide patient lists.
    *   **Longitudinal History**: Detailed view of past Encounters and secure, on-demand decryption and rendering of historical clinical photography.

---

## ⚙️ 3. Cross-Platform Hardware Implementations

ChartCam is built upon a robust Kotlin Multiplatform foundation. While the core business logic, UI layer, and FHIR modeling are 100% unified, hardware interactions are meticulously optimized for each target platform using native bindings:

*   **📱 Mobile (Android & iOS)**: Fully integrates native camera subsystems (CameraX, AVFoundation), precision hardware sensors (CoreMotion, SensorManager) for the clinical leveler, and hardware-backed secure enclaves (EncryptedSharedPreferences, Keychain) for key management and data at rest.
*   **💻 Desktop (JVM) & Web (Wasm)**: The UI and administrative flows are fully functional. Hardware capabilities (camera, sensors) leverage robust fallback mechanisms and external library integrations (e.g., Sarxos Webcam) or web standards (HTML5 MediaDevices), ensuring cross-platform utility without compromising the core experience.

---

## 🔒 4. Data Security, Privacy, & Compliance Posture

ChartCam is engineered to operate within strictly regulated environments (e.g., HIPAA, GDPR). Security is not an afterthought but a foundational design principle.

*   **Strict Device Isolation (No Gallery Pollution)**: Clinical media captured within ChartCam is actively sandboxed. Images are **never** written to the device's unencrypted public storage or native photo galleries (e.g., Apple Photos, Google Photos).
*   **Data Encryption at Rest**: All media and cached data are persistently encrypted using industry-standard AES-256 encryption. The encryption keys are securely managed within the device's hardware-backed keystore/keychain, ensuring data is inaccessible outside the authenticated application context.
*   **Ephemeral Data Lifecycle**: Unassigned media residing in the `/capture` cache is strictly temporary. To mitigate data leakage, the application aggressively purges this cache upon session termination, prolonged inactivity timeouts, or application termination.
*   **FHIR-Native Data Provenance**: Every captured image is accompanied by robust metadata, modeled via FHIR `Provenance` and `Consent` resources, establishing an immutable audit trail of the capturing practitioner, device, and timestamp.
