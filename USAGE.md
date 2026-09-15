# ChartCam Operational Clinical Usage Guide

Welcome to the **ChartCam** operational usage guide. This manual comprehensively details the application's clinical operating workflows, dynamic study instruments, decentralized data management, navigation paradigms, and compliance behaviors for professional healthcare and clinical research environments.

---

## 🧭 Table of Contents
1. [Clinical Workflows & Operating Paradigms](#1-clinical-workflows--operating-paradigms)
2. [Media Triage & Batch Photo Management](#2-media-triage--batch-photo-management)
3. [Operating Dynamic SDC Forms & Clinical Instruments](#3-operating-dynamic-sdc-forms--clinical-instruments)
4. [Questionnaire Builder, Duplication & Study Sharing](#4-questionnaire-builder-duplication--study-sharing)
5. [Multimodal Clinical Capture: Video & Audio Dictation](#5-multimodal-clinical-capture-video--audio-dictation)
6. [Decentralized Data Exchange & Patient Merge Engine](#6-decentralized-data-exchange--patient-merge-engine)
7. [DICOM PACS Export & Built-In DICOM Inspector](#7-dicom-pacs-export--built-in-dicom-inspector)
8. [Security, Privacy Shielding & Inactivity Lockout](#8-security-privacy-shielding--inactivity-lockout)
9. [Localization, RTL & Traditional Chinese Vertical Writing](#9-localization-rtl--traditional-chinese-vertical-writing)
10. [Demo Mode & Exploration](#10-demo-mode--exploration)

---

## 📸 1. Clinical Workflows & Operating Paradigms

ChartCam is designed to eliminate administrative friction at the point of care while supporting rigorous clinical research. To accommodate diverse healthcare scenarios, the platform provides **two primary operating workflows**:

```
                       ┌─────────────────────────────────────┐
                       │       Clinician Authentication       │
                       │   (Biometrics or PIN via Argon2id)   │
                       └──────────────────┬──────────────────┘
                                          │
                  ┌───────────────────────┴───────────────────────┐
                  ▼                                               ▼
      ┌───────────────────────┐                       ┌───────────────────────┐
      │      Workflow A       │                       │      Workflow B       │
      │   "Snap-First" Flow   │                       │ "Protocol-First" Flow │
      │ (High-Acuity Triage)  │                       │ (Scheduled Studies)   │
      └───────────┬───────────┘                       └───────────┬───────────┘
                  │                                               │
                  ▼                                               ▼
      ┌───────────────────────┐                       ┌───────────────────────┐
      │ Immediate Camera Feed │                       │   Patient Directory   │
      │  & Hardware Leveler   │                       │  & Visit History Hub  │
      └───────────┬───────────┘                       └───────────┬───────────┘
                  │                                               │
                  ▼                                               ▼
      ┌───────────────────────┐                       ┌───────────────────────┐
      │   Triage & Attribution│                       │ Dynamic SDC Form &    │
      │  (Batch Select/Delete)│                       │ Inline Media Capture  │
      └───────────┬───────────┘                       └───────────┬───────────┘
                  │                                               │
                  └───────────────────────┬───────────────────────┘
                                          │
                                          ▼
                              ┌───────────────────────┐
                              │  Encrypted On-Device  │
                              │ Persistence (SQL/FHIR)│
                              └───────────────────────┘
```

### Workflow A: "Snap-First" (High-Acuity / Emergency Triage)
In acute trauma, emergency medicine, or rapid field triage, the primary clinical imperative is to capture clinical images instantly before wounds are dressed or pathology changes.
1. **Launch & Authenticate:** Authenticate via local PIN or hardware biometrics (FaceID / TouchID / BiometricPrompt).
2. **Immediate Capture (`/capture`):** The app defaults directly to the hardware camera viewfinder with a real-time accelerometer leveler. Capture photos without pre-registering a patient.
3. **Media Triage (`/triage`):** Advance to the triage gallery to review shots, delete accidental captures, search for an existing patient, or provision a new FHIR `Patient` resource.
4. **Attribution & Encrypted Save:** Selected photos are encrypted on-device, linked to a new FHIR `Encounter`, and encapsulated into `DocumentReference` records.

### Workflow B: "Protocol-First" (Scheduled Intake & Clinical Studies)
In clinical trials, dermatology consultations, or structured outpatient intake, data collection is guided by a predetermined study protocol.
1. **Patient Selection (`/patients`):** Search by name or Medical Record Number (MRN) from the centralized Patient Directory.
2. **Open / Create Visit:** Navigate to the patient's record and start a new clinical encounter (`/patients/{id}/visit`).
3. **Select Study Questionnaire:** Choose the relevant protocol (e.g., Post-Op Wound Assessment, Psoriasis Severity Index).
4. **Guided Data Collection:** Fill dynamic form controls (pain scales, phototyping swatches, body maps), capture targeted photos directly linked to specific questionnaire items, and record voice dictation memos.
5. **Finalize Encounter:** Mark the visit as finalized to preserve an immutable audit record.

---

## 🗂️ 2. Media Triage & Batch Photo Management

The `/triage` interface serves as the buffer between ephemeral captures and permanent medical records:

* **Visual Review Carousel:** Displays clear thumbnails of all media captured during the active session.
* **Granular Batch Selection:** Tap individual photo cards to toggle selection checkmarks, or use **Select All** / **Deselect All** to manage the batch.
* **Immediate Deletion of Accidental Captures:** Tap **Delete Selected** to permanently discard blurred or unusable photos from disk before associating anything with patient records.
* **Multi-Patient Session Partitioning:** Clinicians can photograph multiple wounds or patients in a single session, select a subset of photos for Patient A, confirm association, and then assign the remaining photos to Patient B without leaving the triage screen.
* **On-the-Fly Patient Creation:** If the patient is not yet registered, tap **Create Patient** to provision a full FHIR `Patient` resource (Given name, Family name, MRN, Date of Birth, Gender) without losing the captured media.

---

## 📋 3. Operating Dynamic SDC Forms & Clinical Instruments

ChartCam renders dynamic clinical questionnaires directly from standard HL7 FHIR `Questionnaire` definitions using Compose Multiplatform:

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Dynamic SDC Questionnaire                       │
├────────────────────────────────────────────────────────────────────────┤
│ [Pain Assessment]                                                      │
│  Wong-Baker FACES® & VAS: [ 4 - Hurts Little More  😊 😐 🙁 😢 😭 ]   │
├────────────────────────────────────────────────────────────────────────┤
│ [Dermatological Phototype]                                             │
│  Fitzpatrick Swatches:    [ I ] [ II ] [ III ] [ IV ] [ V ] [ VI ]     │
├────────────────────────────────────────────────────────────────────────┤
│ [Anatomical Location]                                                  │
│  Body-Map Pin Drop:       [ Tap Diagram to Place Pin: (x=142, y=380) ] │
├────────────────────────────────────────────────────────────────────────┤
│ [Calculated Clinical Score]                                            │
│  Height: 180 cm  |  Weight: 75 kg  -->  Auto-Computed BMI: 23.1 kg/m²  │
├────────────────────────────────────────────────────────────────────────┤
│ [Repeating Observations: Lesions]                                      │
│  + Entry 1: Left Forearm (Erythema)                                    │
│  [ + Add Entry ]  [ - Remove Entry ]                                   │
└────────────────────────────────────────────────────────────────────────┘
```

### Specialized Clinical Controls
* **Wong-Baker FACES® & Visual Pain Scale:** An interactive continuous slider spanning 0 ("No Hurt") to 10 ("Hurts Worst") accompanied by validated affective face glyphs and clinical severity labels.
* **Fitzpatrick Skin Phototype Swatches:** Standardized chromatic color tiles allowing dermatologists to classify phototypes I through VI with a single touch.
* **Anatomical Body-Map Pin Drop:** An interactive diagram supporting touch-placed coordinate pins to document the precise physical location of lesions, wounds, or surgical sites.
* **Segmented Visual Choice Tiles:** High-contrast, touch-optimized option cards designed for rapid intake in sterile or gloved clinical environments.
* **Dynamic Repeating Groups:** For clinical records with variable occurrences (e.g., cataloging multiple skin lesions or adverse events), clinicians can tap **+ Add Entry** to dynamically spawn repeated nested question blocks, or tap **- Remove Entry** to prune them.
* **Automated Calculated Expressions:** The embedded `SdcEvaluator` executes arithmetic formulas embedded in `sdc-questionnaire-calculatedExpression` extensions (e.g., automatically deriving Body Mass Index from height and weight fields).
* **Conditional Visibility (`enableWhen`):** Questions dynamically appear or collapse based on answers to preceding trigger questions (e.g., revealing biopsy details only if "Biopsy Performed = Yes").

---

## 🛠️ 4. Questionnaire Builder, Duplication & Study Sharing

Clinicians and clinical research coordinators can configure and deploy bespoke study forms directly on device:

### Building a Form
1. Navigate to **Patient Directory -> Menu -> Questionnaires**.
2. Tap the floating **+** action button to launch the **Questionnaire Builder**.
3. Enter the study title and description.
4. Add questions from the widget selector:
   * *Media:* Photo Camera, Video Camera
   * *Choices:* Segmented Tiles, Single-Select Dropdowns, Multi-Select Checkboxes, Switches
   * *Inputs:* Single-Line Text, Multi-Line Notes, Numeric Fields, Date, DateTime
   * *Specialized:* Pain Scales, Fitzpatrick Palettes, Anatomical Body Maps
5. Configure parameters: Mandatory/Required flags, Min/Max numerical bounds, and conditional `enableWhen` triggers.
6. Tap **Save Questionnaire** to validate and persist the definition as a compliant FHIR `Questionnaire` resource.

### Duplicating & Sharing Studies
* **Duplication:** Tap the duplicate icon on any existing questionnaire card to clone all fields and logic, allowing rapid authoring of trial variants.
* **Sharing via FHIR JSON:** Tap **Share** to serialize the questionnaire into standard FHIR R4 JSON and distribute it via the system ShareSheet (AirDrop, email, Bluetooth, local messaging).
* **Importing via Clipboard:** Tap **Import Questionnaire** and paste valid FHIR R4 Questionnaire JSON to immediately deploy a collaborator's study protocol onto your device.

---

## 🎥 5. Multimodal Clinical Capture: Video & Audio Dictation

Beyond high-resolution photography, ChartCam supports rich clinical media:

### Clinical Video Recording
* Designed for dynamic clinical exams: gait ataxia, tremor assessments, range of motion, and ophthalmic motility.
* Switch between photo and video recording mode directly in the capture interface.
* Videos are encoded as standard `.mp4` payloads, saved to encrypted application storage, and encapsulated as FHIR `DocumentReference` resources (`video/mp4`).

### Clinical Voice Memos & Dictation
* Built-in audio recorder allows hands-free dictation during or immediately after a patient visit.
* Audio memos are recorded directly into encrypted app storage (AAC/M4A on mobile, WAV on desktop) and linked to the active `Encounter` as a FHIR `DocumentReference` (`audio/mp4` or `audio/wav`).

---

## 🔄 6. Decentralized Data Exchange & Patient Merge Engine

ChartCam operates without centralized servers. Moving data between clinical collaborators or backup archives is performed using peer-to-peer mechanisms:

### Encrypted Dataset Backup / Export
1. From the Patient Directory, open the top-right menu and tap **Export Data**.
2. Select your export scope:
   * **All Patients:** Exports the entire local clinic database.
   * **My Patients Only:** Restricts the bundle to patients associated with your practitioner ID.
3. Provide an encryption password (minimum 6 characters).
4. The service compiles all resources and associated binary media into an encrypted archive using **AES-256-GCM** with keys derived via **Argon2id** (RFC 9106).
5. Share the resulting encrypted file via AirDrop, USB drive, or local secure storage.

### Selective Import & Conflict Resolution
When receiving an encrypted dataset from a colleague or backup archive:
1. Tap **Import Data** in the Patient Directory menu and enter the archive password.
2. The application stages the archive and presents the **Import Preview Summary**:
   * Total Patients, Encounters, DocumentReferences, and Questionnaires.
   * Total conflicting patient records (matching MRNs with diverging demographics).
3. For each conflicting record, choose a resolution strategy powered by the `PatientMergeEngine`:
   * **Merge Demographics:** Safely merges missing demographic data (e.g., missing gender, birth date) while retaining your local canonical patient record ID and re-parenting incoming encounters.
   * **Replace Local Record:** Overwrites the local patient record with the incoming data.
   * **Skip:** Preserves the local record and ignores incoming changes.
4. Confirm import to write the merged data safely into the encrypted SQLDelight database.

### CSV Cohort Export
* For biostatisticians and research analysts requiring tabular data, ChartCam exports patient cohorts to standard **RFC 4180 CSV** format, fully compatible with R, Python (Pandas), Excel, and statistical packages.

---

## 🩻 7. DICOM PACS Export & Built-In DICOM Inspector

To integrate seamlessly with hospital Picture Archiving and Communication Systems (PACS) and radiology suites, ChartCam provides full DICOM Part 10 support:

### DICOM Export (.dcm)
* Any clinical encounter and its associated photography can be converted into standard DICOM Part 10 payloads.
* **Anonymization Toggle:** For academic clinical studies and public presentations, enabling the anonymization toggle scrubs Patient Name, MRN, and identifying demographic tags before generating the `.dcm` file.

### Built-In DICOM Inspector
ChartCam includes an interactive DICOM Viewer (`/dicom/viewer`) that requires no external imaging software:
* **Metadata Summary:** Inspect SOP Class UID, Modality (e.g., `XC` for External Camera), Study Datetime, and Patient tags.
* **Interactive Image Canvas:** Renders encapsulated JPEG or raw pixel rasters with full support for **touch drag-to-pan** and **pinch-to-zoom** (1.0x up to 5.0x magnification) for detailed examination of dermatological margins.
* **Encapsulated PDF Preview:** If the DICOM container encapsulates a PDF clinical report, the inspector extracts and displays the document with an integrated action to share or print.

---

## 🔐 8. Security, Privacy Shielding & Inactivity Lockout

ChartCam is engineered to meet the strictest HIPAA, GDPR, and institutional review board (IRB) compliance standards:

* **Zero Network Communication:** The application contains zero remote network connections, zero telemetry, and zero cloud synchronization engines. Your device is the sole sovereign data custodian.
* **Strict Sandboxing (No Photo Gallery Leaks):** Clinical media is saved exclusively within the app’s internal encrypted directory and is **never** accessible to consumer gallery apps (Google Photos, Apple Photos, iCloud).
* **Hardware-Backed Encryption:** Cryptographic keys are protected using platform-native hardware enclaves (Android Keystore, iOS Keychain, POSIX POSIX-restricted file attributes, WebCrypto non-extractable keys).
* **OS-Level Visual Privacy Obscuring:**
  * On Android, window `FLAG_SECURE` is active, blocking the OS task switcher from caching preview thumbnails and preventing screenshots of patient data.
  * On iOS, the app obscures the visual view hierarchy whenever the application moves to the background.
  * On Desktop and Web, window focus loss listeners immediately protect clinical data from shoulder-surfing.
* **Inactivity Auto-Lockout:** If the app remains in the background for longer than **60 seconds**, the active session is locked, requiring biometric re-authentication or PIN entry to regain access.
* **Biometric Authentication:** Supports TouchID, FaceID, and Android Strong BiometricPrompt with cryptographic object binding.
* **Account Deletion & Data Purge:** Clinicians can tap **Delete My Account** to irreversibly wipe local database records, purges credentials, and clear all cached clinical media.

---

## 🌐 9. Localization, RTL & Traditional Chinese Vertical Writing

ChartCam is fully internationalized to support global research teams:

* **Supported Languages:** English (`en`), Spanish (`es`), Japanese (`ja`), Hebrew (`he`), and Traditional Chinese (`zh-Hant`).
* **Right-to-Left (RTL) Mirroring:** When Hebrew is selected, the application automatically flips navigation trees, back buttons, inputs, and layout directions to full RTL.
* **Traditional Chinese Vertical Column Writing (直書 / 豎排):**
  * In addition to standard horizontal text, Traditional Chinese supports authentic vertical column layout where characters flow top-to-bottom and columns progress right-to-left.
  * Clinicians can toggle between vertical column banners and horizontal text layouts directly within the interface.
* **Localized Clinical Dates:** All FHIR `date` and `dateTime` values are formatted according to the active locale (e.g., `YYYY年MM月DD日` in Japanese vs `DD/MM/YYYY` in European Spanish).

---

## 🧪 10. Demo Mode & Exploration

For medical educators, trainees, or evaluators who want to test ChartCam without handling Protected Health Information (PHI):

1. On the **Login Screen**, tap **Explore Demo Mode**.
2. The `DemoDataSeeder` automatically provisions a realistic simulated clinical environment:
   * Sample patients with diverse demographic profiles.
   * Historical encounters with simulated wound photographs.
   * Bundled study questionnaires (e.g., Fitzpatrick phototyping, visual pain scales, body maps).
3. A distinct **Demo Mode Banner** remains visible throughout the session.
4. When done, tap **Exit Demo** to purge all demo records and return to the secure login prompt.
