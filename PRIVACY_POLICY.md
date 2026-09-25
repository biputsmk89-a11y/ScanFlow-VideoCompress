# Privacy Policy - CompressFlow

**Effective Date:** September 25, 2026  
**Last Updated:** September 25, 2026  

This Privacy Policy explains how **CompressFlow** ("we", "our", or "the app") handles user data and permissions. CompressFlow is designed with privacy at its core: all video compression, trimming, and audio processing occur **100% locally on your device**.

> **Core Guarantee:** CompressFlow does NOT collect, upload, store on remote servers, sell, or share your personal videos, photos, or files. Everything stays on your phone.

---

## 1. Information We Do NOT Collect
Unlike cloud-based conversion utilities, CompressFlow operates completely offline:
- **No User Account:** You do not need to register, log in, or provide an email, phone number, or name to use the app.
- **No Video/Media Collection:** Your original videos, processed videos, and extracted audio are never transmitted to any external server or cloud storage.
- **No Personal Identifiers:** We do not track or store your device identifiers (IMEI, advertising ID, MAC address).
- **No Location Data:** The app does not access, collect, or store your GPS or network location.

---

## 2. Device Permissions Used and Purpose
CompressFlow requests only the minimum necessary permissions strictly required to perform video processing functions:
- **Photos and Videos / Media Access (`READ_MEDIA_VIDEO` / `READ_EXTERNAL_STORAGE`):** Required solely to allow you to select videos from your gallery for compression and save the resulting compressed videos to your device storage.
- **Foreground Service (`FOREGROUND_SERVICE_MEDIA_PROCESSING`):** Allows the compression engine to continue processing your video reliably in the background without being terminated by Android's battery optimizer.
- **Notifications (`POST_NOTIFICATIONS`):** Optional on Android 13+. Used solely to display real-time compression progress and notify you when your video is ready.

---

## 3. Third-Party Services and SDKs
CompressFlow does not incorporate third-party analytics trackers, advertising networks, or data brokers. All video encoding and decoding are powered by local Android Media3 / MediaCodec hardware accelerators.

---

## 4. Data Storage and Retention
All compression history entries and user settings (such as compression presets, dark mode preferences) are stored strictly locally on your device using a private SQLite/Room database. You can clear this data at any time from within the app settings or by clearing the app's storage in Android settings.

---

## 5. Children's Privacy
CompressFlow does not knowingly collect any personal identifiable information from children under the age of 13. Since no personal data is collected from any user, CompressFlow complies with COPPA and global child privacy regulations.

---

## 6. Security
Because your videos never leave your device, your files are protected by your operating system's built-in sandbox security. We do not maintain any cloud databases that could be breached or compromised.

---

## 7. Changes to This Privacy Policy
We may update this Privacy Policy from time to time. Any changes will be posted on this page with an updated revision date.

---

## 8. Contact Us
If you have any questions, feedback, or concerns regarding this Privacy Policy, please contact us:
- **Developer:** biputsmk89-a11y
- **Repository:** [https://github.com/biputsmk89-a11y/ScanFlow-VideoCompress](https://github.com/biputsmk89-a11y/ScanFlow-VideoCompress)
