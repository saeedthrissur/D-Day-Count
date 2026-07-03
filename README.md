# 📅 D-Day Count

A privacy-first, fully featured open-source countdown and count-up calendar application for Android. Built with Jetpack Compose, Clean Architecture, and Room Database.

---

## ✨ Features

- **Privacy-First:** Completely offline-capable. No trackers, no analytics, and zero ads.
- **Dynamic Sizing Widgets:** Highly configurable home screen widgets that adapt beautifully to your layout.
- **Advanced Layout & Aesthetics:** Support for custom background photos (with built-in offline blur/crop filters), gradient layers, and asset stickers.
- **Smart Calculations:** Simple countdown mode for short durations, shifting automatically to detailed breakdowns (Years, Months, Weeks, Days) for long-term milestones.
- **Local Data Freedom:** Secure client-side import and export via Excel-compatible CSV sheets or full-state JSON configuration backups.
- **Group Management:** Organise milestones seamlessly with custom filterable groups and quick sort controls.

---

## 🚀 Build Flavors

This project uses Gradle Product Flavors to stay 100% compliant with F-Droid's open-source policies while allowing extended features for alternative builds:

1. **`foss` (Free and Open-Source):** Built strictly with open-source dependencies. Fully decoupled from proprietary background networks—perfect for F-Droid.
2. **`google` (Extended Feature Platform):** Includes optional integrations for Google AI Studio / Gemini API infrastructure and runtime security configurations.

---

## 🛠️ How to Build & Run Locally

### Prerequisites
- [Android Studio](https://developer.android.com/studio) (Latest Version)
- Android SDK 24 or higher

### Local Setup
1. Clone this repository to your local workspace or download and extract the source zip.
2. Open **Android Studio**, select **Open**, and navigate to the project root directory.
3. Allow Gradle to synchronize and resolve the structural properties.
4. **For the `google` variant:** Create a `.env` file in the root directory and add your secret token key as specified in `.env.example`.
5. Select your preferred build variant (`fossDebug` or `googleDebug`) via the **Build Variants** panel in Android Studio.
6. Connect an Android device or launch an emulator, then click **Run**.

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0 (GPLv3)** - see the LICENSE file for details.
