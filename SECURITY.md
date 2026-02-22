# ChartCam Security Architecture

This document outlines the security design principles, threat models, and mitigation strategies implemented in the **ChartCam** application. Because ChartCam handles Protected Health Information (PHI) and operates within clinical environments, it is engineered to comply with HIPAA regulations by ensuring data confidentiality, integrity, and availability across all supported platforms.

## 1. Security Design Principles

ChartCam is built on a "Zero Trust" and "Defense in Depth" philosophy. The core principles include:

1.  **Encryption at Rest:** All PHI (databases, clinical photos, session tokens) must be encrypted on the local file system. Plaintext PHI is never stored on disk.
2.  **Secure Authentication:** User sessions are protected by cryptographic hashing and secure token storage.
3.  **Platform-Specific Hardening:** The application leverages the native security enclaves of each operating system (e.g., Android Keystore, iOS Keychain) to protect encryption keys.
4.  **No Data Leakage:** The application actively prevents OS-level data leakage, such as background screen captures or unencrypted cache files.

---

## 2. Threat Model & Mitigations

### 2.1. Physical Device Compromise (Lost or Stolen Device)
**Threat:** An attacker gains physical access to a clinician's device and attempts to extract the local database or cached patient photos via USB debugging (ADB), jailbreaking, or rooting.

**Mitigation:**
*   **Database Encryption:** The local SQLite database is encrypted using **SQLCipher**. The database file itself is unreadable without the 256-bit AES key.
*   **File Encryption:** Clinical photos captured during an encounter are not saved as standard JPEGs. They are encrypted using platform-specific APIs (e.g., `EncryptedFile` on Android) using AES256-GCM.
*   **Key Protection:** The AES keys used for SQLCipher and File Storage are not hardcoded. They are generated cryptographically and stored securely in the hardware-backed Trusted Execution Environment (TEE) of the device.

### 2.2. Offline Dictionary / Brute Force Attacks
**Threat:** An attacker extracts the local secure storage and attempts to crack the stored password hashes to gain unauthorized access to the application offline.

**Mitigation:**
*   **Strong Hashing:** Passwords are not stored in plaintext. They are salted and hashed using SHA-256 (via Okio) before being compared or stored.
*   *(Future Enhancement)*: While SHA-256 is currently used as a baseline, migrating to a specialized Key Derivation Function (KDF) like Argon2 or PBKDF2 is recommended for production to further slow down brute-force attempts.

### 2.3. Timing Attacks on Authentication
**Threat:** An attacker measures the exact time it takes for the application to reject a login attempt. By analyzing these microsecond differences, the attacker can guess the length or partial contents of the valid password.

**Mitigation:**
*   **Constant-Time Comparison:** The `AuthRepository` implements a custom `constantTimeEquals` function. Instead of returning early when a character mismatch occurs or when lengths differ, the function always iterates through the entire string (or a dummy string of equal length), ensuring the execution time remains constant regardless of the input.

### 2.4. OS-Level Data Leakage (Screen Recording / App Switcher)
**Threat:** A clinician switches apps, and the OS takes a snapshot of the screen to display in the "Recent Apps" switcher, inadvertently saving PHI to the unencrypted OS cache. Alternatively, malware attempts to record the screen.

**Mitigation:**
*   **Window Flags:** On Android, the `FLAG_SECURE` window layout parameter is set immediately in `MainActivity.kt`. This prevents the OS from taking screenshots, blocks screen recording apps, and blackouts the app preview in the recent apps list.

---

## 3. Platform-Specific Security Implementations

Because ChartCam is a Kotlin Multiplatform (KMP) application, it relies on `expect`/`actual` declarations to bridge common security requirements to the native security APIs of each operating system.

| Target               | Database Encryption (SQLCipher) | File Encryption (Photos) | Key Storage (Tokens & Passphrases) | OS-Level Hardening |
|:---------------------|:--------------------------------|:-------------------------|:-----------------------------------|:-------------------|
| **🤖 Android**       | `AndroidSqliteDriver` + `SupportFactory` | Jetpack Security `EncryptedFile` | Jetpack Security `EncryptedSharedPreferences` | `WindowManager.LayoutParams.FLAG_SECURE` |
| **🍎 iOS**           | Native SQLite + SQLCipher framework | iOS Data Protection API (`NSFileProtectionComplete`) | iOS Keychain Services | App Delegate snapshot blurring (Planned) |
| **🖥️ Desktop (JVM)** | `JdbcSqliteDriver` (Requires SQLCipher JDBC) | AES-GCM via `javax.crypto.Cipher` | Java `Preferences` API (Payloads encrypted via AES-GCM) | N/A |
| **🌐 Web (JS/Wasm)** | Web Worker w/ encrypted virtual fs | Encrypted Blobs/IndexedDB | Web Crypto API / Encrypted LocalStorage | N/A |

### Implementation Notes:
*   **Android Initialization:** The Android application must call `AndroidAppInit.init(this)` during `onCreate` to provide the application `Context` to the KMP shared library. This context is strictly required to access the Android Keystore and the filesystem safely.
*   **Desktop JVM Limitation:** Standard SQLite JDBC drivers do not support transparent encryption. For a true HIPAA-compliant desktop build, a custom compiled JDBC driver linking against SQLCipher is required. Currently, the JVM implementation encrypts individual preference payloads but relies on the host OS for full-disk encryption.