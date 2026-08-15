# Charging Clock

An Android application that automatically activates the Always-On Display while your phone is charging.

<img src="screenshot.png" alt="Ambient Control status dashboard" width="320"/>

> [!NOTE]
> **AI Notice**: this project was updated for newer phones by Claude, in 2026

## Supported devices

Samsung devices running One UI with Always-On Display. On other devices the app
installs but reports "Not detected" and does nothing.

## Installation

Until it's published, build and install it yourself.

### Building

You'll need the Android SDK (compile SDK 35) and a device running Android 7.0 (API 24) or later.

```
$ ./gradlew assembleDebug
```

The APK ends up in `app/build/outputs/apk/debug/app-debug.apk`.

For release builds and Play Store publishing, see [docs/PUBLISHING.md](docs/PUBLISHING.md).

### Installing

With USB debugging enabled on your device:

```
$ ./gradlew installDebug
```

Or install the APK directly:

```
$ adb install app/build/outputs/apk/debug/app-debug.apk
```

### Granting permissions

Grant the "Modify System Settings" permission. It should be offered on first launch, and can be reached via System Settings manually under `Settings > Apps > Special app access`.

The _Always-On Display_ is toggled with charging state, **but the mode when enabled must still be set by you** (the intended is _Always_).

No ADB commands required.
