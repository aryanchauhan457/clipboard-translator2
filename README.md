# Clipboard Translator (Android)

A small native Kotlin Android app with a movable floating button. Copy text in another app, tap the floating `हि` bubble, and the app opens a foreground Activity to read the clipboard, translate English to Hindi, and show a notification.

## Build an APK using GitHub Actions (no Android Studio required)

1. Create a new GitHub repository, for example `clipboard-translator`.
2. Upload all files and folders from this project to the repository root. Keep `.github/workflows/build-apk.yml` in that exact location.
3. Commit the files to the `main` branch.
4. Open the repository on GitHub and select **Actions**.
5. Select **Build Clipboard Translator APK** from the workflow list.
6. Click **Run workflow** (or wait for the automatic build after pushing to `main`).
7. Open the completed workflow run. Scroll to **Artifacts** and download `ClipboardTranslator-debug-apk`.
8. Extract the downloaded ZIP. It contains `app-debug.apk`.
9. Transfer the APK to your Android phone, open it, and allow installation from that source.

## First launch

1. Open Clipboard Translator.
2. Tap **Enable floating button**.
3. Android opens the overlay permission screen. Allow **Display over other apps**.
4. Return to the app and tap **Enable floating button** again.
5. Copy English text in another app.
6. Tap the floating blue `हि` bubble. The app opens in the foreground, reads the clipboard, translates it, and posts a notification.
7. Drag the bubble to reposition it. Use **Stop floating button** in the app to remove it.

## Important limitations

- Clipboard is read only after you tap the bubble and the app's Activity comes to the foreground. It does not poll the clipboard in the background.
- Translation currently uses Google's unofficial `translate.googleapis.com` web endpoint. It needs internet access, may be rate-limited, and is not a supported production API. For reliable production use, replace it with Google Cloud Translation API and credentials.
- The foreground service uses Android's `specialUse` foreground-service type to keep the user-requested floating bubble alive. Newer Android versions may show additional system notices or impose manufacturer-specific restrictions.
- This is a debug APK, not a Play Store release. It is not signed with a private release key.
- GitHub Actions builds the APK; no Android Studio installation is needed on your PC.

## Project configuration

- Kotlin
- Android Gradle Plugin 8.7.3
- Gradle 8.9
- JDK 17
- compileSdk 35 / targetSdk 35 / minSdk 26
