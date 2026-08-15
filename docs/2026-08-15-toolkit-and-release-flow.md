# 2026-08-15 — Project toolkit and release flow

Decisions from design discussion, and the implementation plan for `bin/chclock`.

## Goals

Capture build commands and automate release preparation. Three commands plus help:

- `build [--variant debug|release] [--install]`
- `capture [state-name]`
- `prepare --version X.Y.Z --changes <markdown> [--screenshots]`

## Decisions and rationale

### Toolkit

- **Python 3, single file `bin/chclock`, stdlib only.** Bash is always present, but
  `prepare` needs interactive prompts, validation, git operations and file edits —
  python's error handling is worth it. Host (macOS) and container both have python3.
- Build outputs copied to `dist/chclock-<variant>-<version>.apk` (plus `.aab` for
  release). On failure, print the tail of the build log. `dist/` and `secrets/` are
  gitignored.

### Mocking for screenshots (`capture`)

- **Rejected**: a `mock` buildType with a separate activity (layout drift risk,
  third buildType for nothing), and mocking at the `Ambient` level (can't fake
  charger or battery-optimization rows, which come from `PowerUtils`).
- **Chosen**: a `MainPresenter` interface backing `MainActivity` — typed properties
  (plugged, supported, permission, AOD on, style, battery, enabled) and actions.
  - `RealMainPresenter` delegates to `Ambient` / `PowerUtils` / `Prefs`.
  - `MockMainPresenter` holds in-memory values; actions mutate memory or no-op.
  - `StatusItems.build(presenter)` assembles the dashboard rows from the presenter,
    so real and mock runs exercise identical UI and hint logic.
- Named presets live in `MockMainPresenter` (`perfect`, `unplugged`, `no-permission`,
  `wrong-style`, `battery-restricted`, `not-supported`, `disabled`). Selected via
  intent extra `--es preset <name>`, honored only in debug builds
  (`BuildConfig.DEBUG`). Release path untouched.
- Presenter scope: `MainActivity` only. Tile and control service keep using
  `Ambient` directly.
- `Ambient.getStatus()` removed — status assembly now lives in `StatusItems`.
- Debug builds get `applicationIdSuffix ".debug"` so a debug install never collides
  with the store install on a personal device.
- **Device**: emulator cannot run in the container. `capture` targets whatever adb
  device is available — host emulator or a real test device (safe: mock presenter
  touches no real settings). From the container, the toolkit falls back to the host
  adb server via `ADB_SERVER_SOCKET=tcp:host.docker.internal:5037`; if no device is
  reachable it fails with a clear message.

### Signing (`prepare`)

- `keystore.properties` is gone. Keystore lives at `secrets/release.keystore`
  (untracked), key alias `chclock`.
- Password is prompted interactively, validated with `keytool -list`, and passed to
  Gradle via the `KEYSTORE_PASSWORD` environment variable of the child process only.
  Env var, not `-P` flag, so it never shows in `ps`. Never on disk.
- Consequence: **`prepare` runs on the host only** (interactive prompt; password
  goes password-manager → host memory → forgotten). A pre-flight check at the top
  verifies: interactive tty, clean git tree, keystore file, keytool, gradle/JDK,
  rsvg-convert, and (with `--screenshots`) an adb device. No container support for
  `prepare` — deliberate, not planned.
- `capture` and `build` can run anywhere.

### Versioning

- `versionName X.Y.Z`, `versionCode = X*10000 + Y*100 + Z` (caps minor/patch at 99 —
  accepted). Written to `app/build.gradle` only; manifest versions rejected (AGP
  overwrites them at build — source values would be ignored noise).
- Changes prepended to `docs/VERSIONS.md` as `## X.Y.Z — YYYY-MM-DD` + body.
- On green release build: commit the bump + VERSIONS.md, tag `vX.Y.Z`. On failure:
  error report, tree left dirty.
- `prepare` also renders `art/icon.svg` to `dist/icon-512.png` for the Play listing.

### Release rules (also in CLAUDE.md)

- When the user indicates release time, the agent's job is to generate the
  `bin/chclock prepare ...` command for copy-paste on the host — never run it.
- Screenshots: retake only if UI changed since last version (human/agent judgment).
- Lock-screen store screenshot: static image, faked once from a Samsung reference
  photo — TODO, waiting on photo.
