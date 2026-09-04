# Upgrading ChartCam Android via CLI (ADB)

This guide provides step-by-step instructions for upgrading ChartCam on an Android device using the command-line interface (CLI) with **Android Debug Bridge (ADB)**.

Use this guide if you encounter the following installation error when updating:
> **"App not installed. Package conflicts with an existing package."**

This error occurs when the new APK was signed with a different key than the version currently on your device. Following the steps below allows you to back up your patient data, remove the conflicting installation, install the updated APK, and restore all data seamlessly.

---

## Table of Contents
1. [Prerequisites: Installing ADB on Your Computer](#1-prerequisites-installing-adb-on-your-computer)
2. [Enabling Developer Options and USB Debugging](#2-enabling-developer-options-and-usb-debugging)
3. [Connecting Your Device and Authorizing ADB](#3-connecting-your-device-and-authorizing-adb)
4. [Backing Up Existing Patient Data](#4-backing-up-existing-patient-data)
5. [Uninstalling the Old Version](#5-uninstalling-the-old-version)
6. [Installing the New ChartCam APK](#6-installing-the-new-chartcam-apk)
7. [Restoring Your Patient Data](#7-restoring-your-patient-data)
8. [Verifying the Upgrade](#8-verifying-the-upgrade)
9. [Troubleshooting Common Issues](#9-troubleshooting-common-issues)

---

## 1. Prerequisites: Installing ADB on Your Computer

ADB (Android Debug Bridge) is a command-line tool that lets your computer communicate with your connected Android phone or tablet.

### macOS
If you use [Homebrew](https://brew.sh/):
```bash
brew install android-platform-tools
```
Alternatively, if you have Android Studio installed, ADB is located at:
```bash
~/Library/Android/sdk/platform-tools/adb
```
Add it to your PATH:
```bash
echo 'export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### Windows
1. Download the official [Android SDK Platform-Tools for Windows](https://developer.android.com/tools/releases/platform-tools).
2. Extract the downloaded ZIP file (e.g., to `C:\platform-tools`).
3. Open PowerShell or Command Prompt.
4. Add to your temporary session PATH, or navigate into the folder:
   ```powershell
   cd C:\platform-tools
   .\adb.exe version
   ```
*(Optional: Add `C:\platform-tools` to your Windows System Environment Variables `PATH` to run `adb` from any directory).*

### Linux (Debian / Ubuntu / Fedora)
- **Ubuntu / Debian:**
  ```bash
  sudo apt update && sudo apt install -y android-tools-adb
  ```
- **Fedora / RHEL:**
  ```bash
  sudo dnf install -y android-tools
  ```
- **Arch Linux:**
  ```bash
  sudo pacman -S android-tools
  ```

### Verify ADB Installation
Run:
```bash
adb version
```
Expected output:
```text
Android Debug Bridge version 1.0.41 (or higher)
```

---

## 2. Enabling Developer Options and USB Debugging

By default, Android devices prevent external computers from accessing internal app directories. You must enable USB Debugging.

### Step 2.1: Enable Developer Options
1. Open the **Settings** app on your Android device.
2. Scroll down and tap **About phone** (or **About tablet**).
3. Find the entry labeled **Build number**.
   - *Note for Samsung:* Go to **About phone** > **Software information** > **Build number**.
   - *Note for Xiaomi / MIUI:* Tap **OS version** or **MIUI version**.
4. Tap **Build number** quickly **7 times in a row**.
5. You will see a toast notification: *"You are now a developer!"* (Enter your lock screen PIN/pattern if prompted).

### Step 2.2: Enable USB Debugging
1. Go back to the main **Settings** menu.
2. Tap **System** > **Developer options** (on Samsung: Settings > Developer options at the very bottom).
3. Find **USB debugging** and toggle the switch to **ON**.
4. Tap **OK** on the warning prompt.

---

## 3. Connecting Your Device and Authorizing ADB

1. Connect your Android device to your computer using a high-quality USB cable.
2. Set the USB connection mode to **File Transfer / Android Auto** or **MTP** (not "Charging only").
3. Unlock your Android device screen.
4. On your computer terminal, run:
   ```bash
   adb devices
   ```
5. Look at your Android device screen! A dialog will pop up:
   > **Allow USB debugging?**
   > Computer's RSA key fingerprint is: `XX:XX:XX:...`
6. Check the box **"Always allow from this computer"** and tap **Allow**.
7. Run `adb devices` on your computer again:
   ```bash
   adb devices
   ```
   Output should show your device ID followed by `device`:
   ```text
   List of devices attached
   RFCW207HXYZ    device
   ```
   *(If it says `unauthorized`, unlock your device and tap "Allow" on the prompt).*

---

## 4. Backing Up Existing Patient Data

ChartCam debug builds are marked as debuggable, which allows extracting your application's encrypted SQLite database, encryption passphrases, and photos directly from the app sandbox without rooting your device.

Run the following command on your computer to save all app data into a compressed archive named `chartcam_backup.tar.gz`:

### macOS / Linux
```bash
adb exec-out run-as io.healthplatform.chartcam tar -czf - databases files shared_prefs cache > chartcam_backup.tar.gz
```

### Windows (PowerShell)
```powershell
adb exec-out run-as io.healthplatform.chartcam tar -czf - databases files shared_prefs cache > chartcam_backup.tar.gz
```

### Verify Backup
Check that the backup archive was created and contains data:
- **macOS / Linux:**
  ```bash
  tar -ztvf chartcam_backup.tar.gz
  ```
- **Windows:**
  ```powershell
  tar -ztvf chartcam_backup.tar.gz
  ```
You should see entries such as:
- `databases/chartcam_encrypted.db`
- `shared_prefs/db_secure_prefs_v2.xml`
- `files/` or `cache/` (containing captured `.jpg` files)

---

## 5. Uninstalling the Old Version

Once your backup archive is confirmed, uninstall the conflicting version from your device:

```bash
adb uninstall io.healthplatform.chartcam
```
Output:
```text
Success
```

---

## 6. Installing the New ChartCam APK

Locate the new APK you downloaded (e.g., `ChartCam-Android-Debug.apk` from the latest release or local build):

```bash
adb install -r path/to/ChartCam-Android-Debug.apk
```

Example:
```bash
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
```
Output:
```text
Performing Streamed Install
Success
```

---

## 7. Restoring Your Patient Data

Now restore the backed-up databases, keys, and photos into the newly installed app sandbox:

1. **Push the backup archive to the device's temporary storage:**
   ```bash
   adb push chartcam_backup.tar.gz /data/local/tmp/chartcam_backup.tar.gz
   ```

2. **Extract the archive directly into the ChartCam data directory:**
   ```bash
   adb shell "run-as io.healthplatform.chartcam tar -xzf /data/local/tmp/chartcam_backup.tar.gz -C /data/data/io.healthplatform.chartcam/"
   ```

3. **Clean up the temporary archive from the device:**
   ```bash
   adb shell rm /data/local/tmp/chartcam_backup.tar.gz
   ```

---

## 8. Verifying the Upgrade

1. **Launch ChartCam:**
   ```bash
   adb shell monkey -p io.healthplatform.chartcam -c android.intent.category.LAUNCHER 1
   ```
   Or simply tap the **ChartCam** app icon on your device screen.

2. **Check Your Data:**
   - Verify that your patients and practitioners are listed.
   - Open patient encounters and confirm previous clinical photos and questionnaire responses appear.
   - Any photos previously stored in the temporary cache will be automatically migrated to persistent internal storage.

3. **Future Upgrades:**
   Because all subsequent builds now share the repository's permanent debug keystore (`androidApp/debug.keystore`), all future updates can be installed directly with a single command without needing to back up or uninstall:
   ```bash
   adb install -r ChartCam-Android-Debug.apk
   ```

---

## 9. Troubleshooting Common Issues

### Issue 1: `adb: device unauthorized`
- **Cause:** You haven't approved the RSA key prompt on your Android screen.
- **Fix:** Unlock your phone screen, check "Always allow from this computer", and tap **Allow**. If the prompt does not appear, run `adb kill-server && adb start-server` and reconnect the USB cable.

### Issue 2: `adb: device not found` or `List of devices attached (empty)`
- **Fix:**
  1. Ensure the USB cable supports data transfer (some cables are charge-only).
  2. In Android Notification shade, tap "USB charging this device" and change it to "File Transfer" or "MTP".
  3. On Windows, install the [Google USB Driver](https://developer.android.com/studio/run/win-usb) or your device manufacturer's OEM driver.

### Issue 3: `run-as: package not debuggable: io.healthplatform.chartcam`
- **Cause:** The installed APK was built in `Release` mode without debug flags.
- **Fix:** `run-as` only works on `Debug` builds (which GitHub Releases distribute as `ChartCam-Android-Debug.apk`). For release builds, use the in-app **Export dataset** feature (Menu > Export dataset) before uninstalling.

### Issue 4: `tar: error exit delayed from previous errors`
- **Fix:** If some optional directories (e.g. `cache`) were empty, tar may emit a warning. Verify with `tar -ztvf chartcam_backup.tar.gz` that `databases/` and `shared_prefs/` are present.
