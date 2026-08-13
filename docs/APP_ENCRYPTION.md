# App Encryption Documentation

This document provides details on the encryption algorithms and implementations used within the ChartCam iOS application, in compliance with Apple's requirements for apps utilizing non-exempt encryption.

## 1. Overview
ChartCam handles sensitive medical data (such as patient lists and questionnaires) and ensures that data remains secure both at rest and in transit. To achieve this, the application utilizes a combination of Apple's built-in operating system encryption frameworks and a standard, internationally recognized key derivation algorithm bundled within the app.

## 2. Use of Apple Operating System Encryption
The application heavily relies on the encryption services provided natively by the iOS operating system:
* **Data in Transit:** Uses standard **HTTPS/TLS** provided by Apple's Foundation framework (via the Ktor networking client) to secure all network communications.
* **Symmetric Encryption:** Uses Apple's **CommonCrypto** framework (`CCCryptor`) to perform **AES-256-GCM** encryption for securing local strings and data payloads.
* **Secure Storage:** Uses iOS **Keychain Services** (`SecItemAdd`, `SecItemUpdate`, `SecItemCopyMatching`) via the `kSecClassGenericPassword` class to securely store authentication credentials and sensitive keys.
* **Random Number Generation:** Uses `SecRandomCopyBytes` to generate cryptographically secure salts and Initialization Vectors (IVs).

## 3. Use of Bundled Standard Encryption Algorithms
In addition to Apple's native frameworks, the app bundles the following standard encryption algorithm:

* **Argon2 (Argon2id, Version 13):**
  * **Purpose:** Used for secure cryptographic key derivation from user-provided passwords.
  * **Standardization:** Argon2 is the winner of the Password Hashing Competition (PHC) and is formally standardized by the IETF as **RFC 9106**.
  * **Implementation:** The app bundles the standard C reference implementation of Argon2, compiled via Kotlin/Native C-Interop (`argon2_hash`). This is included because iOS CommonCrypto does not natively provide an API for Argon2.

## 4. Proprietary Algorithms
* **None.** The app does not use any proprietary, custom, or non-standard encryption algorithms. All cryptographic operations rely on well-established international standards (AES, TLS, Argon2).

## 5. Info.plist Configuration
In accordance with Apple's export compliance guidelines, the following key should be present in the application's `iosApp/iosApp/Info.plist`:
```xml
<key>ITSAppUsesNonExemptEncryption</key>
<true/>
```

---
*Note: Because this application implements a standard encryption algorithm (Argon2 / RFC 9106) in addition to using the encryption within Apple's operating system, it requires this documentation for App Store Export Compliance.*
