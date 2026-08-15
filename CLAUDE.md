# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

# Charging Clock

Android app that activates Samsung's Always-On Display while the phone is charging. One UI devices only; on other devices it installs, reports "Not detected", and does nothing.

## Docs

1. `docs/PUBLISHING.md` — Play Store release: signing, foreground-service `specialUse` declaration, policy risks, per-release chores.
2. `docs/VERSIONS.md` — changelog, maintained by `bin/chclock prepare`.
3. `docs/2026-08-15-toolkit-and-release-flow.md` — toolkit design decisions (presenter mock, signing flow, host-only prepare).

## Branches

- `master` — original "AmbientControl" version (AOSP `doze_always_on` backend, ADB-granted `WRITE_SECURE_SETTINGS`).
- `play-store-version` — current work: Samsung-only, no ADB permissions, rebranded, toolkit. Target for Play publishing.

## Toolkit: `bin/chclock`

Preferred entry point for all project tasks (python3, stdlib only):

```
bin/chclock build [--variant debug|release] [--install]
    # outputs dist/chclock-<variant>-<version>.apk (+ .aab for release)
bin/chclock capture [state]
    # screenshots mocked UI states via adb into dist/screenshots/
    # states: perfect unplugged no-permission wrong-style battery-restricted not-supported disabled all-warnings
bin/chclock probe
    # read-only adb inspection: AOD settings entry points on connected device
bin/chclock prepare --version X.Y.Z --changes "<markdown>" [--screenshots]
    # HOST ONLY. Prompts keystore password, bumps versions, updates VERSIONS.md,
    # renders dist/icon-512.png, builds signed release, commits + tags vX.Y.Z
bin/chclock help
```

Raw gradle still works (`./gradlew assembleDebug|test|connectedAndroidTest`); tests are stubs only.

## Commits

- Commit progressively while working: small commits that tell the implementation story, not one lump at the end.
- Message style: single line, imperative, first letter uppercase, no trailing period (see git log).

## Release rules

- When the user indicates release time: generate the `bin/chclock prepare ...` command for copy-paste on the host. NEVER run it yourself — it is interactive and host-only (keystore password goes password-manager → host memory → forgotten; pre-flight enforces tty + clean git tree).
- `--screenshots`: include only if the UI changed since the last version. Judgment call, not automated.
- Signing: keystore at `secrets/release.keystore` (gitignored), alias `chclock`, password via `KEYSTORE_PASSWORD` env var only. No password on disk, no `-P` flags (visible in `ps`).
- versionCode scheme: `X*10000 + Y*100 + Z` (minor/patch ≤ 99).
- Keep the FGS `specialUse` justification stable between releases (re-reviewed each time).
- Play listing icon: `dist/icon-512.png`, rendered from `art/icon.svg` by prepare.
- Lock-screen store screenshot: static fake, TODO — waiting on a Samsung reference photo from the user.

## Tech stack

- Pure Java, no Kotlin.
- Plain Android SDK, minimal AndroidX (appcompat, constraintlayout). No DI, no architecture framework.
- compileSdk/targetSdk 35, minSdk 24.

## File tree

```
bin/chclock                         # project toolkit (see above)
app/src/main/java/io/salezica/chclock/
  MainActivity.java                 # dashboard view; picks Real or Mock presenter (debug + "preset" extra)
  ui/
    MainPresenter.java              # typed props + actions backing MainActivity
    RealMainPresenter.java          # delegates to Ambient/PowerUtils/Prefs
    MockMainPresenter.java          # in-memory fake; named presets for screenshots
    StatusItems.java                # builds dashboard rows from a presenter (shared real/mock path)
  ambient/
    Ambient.java                    # backend interface: isSupported/hasPermissions/setAlwaysOn/isAlwaysOn/getStyle
    AodStyle.java                   # AOD style enum (ALWAYS, TAP_TO_SHOW, ...) with string-resource labels
    AmbientProvider.java            # backend selector; DEBUG flag switches to mock
    AmbientSamsung.java             # real backend: Settings.System "aod_mode" key
    AmbientMock.java                # SharedPreferences-backed fake backend
    StatusItem.java                 # dashboard row model: label/value/tone/hint/fix-Runnable
  services/
    AmbientControlService.java      # sticky foreground service; power-state receiver + apply logic
    AmbientTileService.java         # Quick Settings tile: manual AOD toggle
  inspection/
    SettingsReader.java             # dev tool: polls & diffs all Settings tables to discover keys
  utils/
    PowerUtils.java                 # charger state, battery-optimization checks/settings intent
    Prefs.java                      # single "enabled" boolean in SharedPreferences
    TaggedLog.java                  # Log.d wrapper with class-name tag
docs/                               # guides + dated decision records
```

## Architecture

- Core flow: `AmbientControlService` (sticky FGS) registers a runtime receiver for `ACTION_POWER_CONNECTED/DISCONNECTED` (manifest receivers can't get these since API 26 — the whole reason the FGS exists). On event: `applyPowerState()` sets AOD = plugged && Prefs.enabled.
- `AmbientControlService.applyPowerState()` is static and also called synchronously from the UI (via `RealMainPresenter.setEnabled`), so toggles apply before the dashboard re-renders.
- UI seam: `MainActivity` renders `new StatusItems(context, presenter).build()`. All natural language lives in `strings.xml`; Java holds only identifiers and enum values. `RealMainPresenter` reads the system; `MockMainPresenter` fakes it for `bin/chclock capture` (debug builds only, selected by `--es preset <name>` intent extra; skips starting the FGS). Preset names in `MockMainPresenter.PRESETS` must stay in sync with `STATES` in `bin/chclock`.
- `Ambient` interface abstracts the AOD backend; `AmbientProvider.getFor()` is the only construction point. Tile + control service use it directly (no presenter).
- Debug builds use `applicationIdSuffix ".debug"` — never collides with a store install; adb commands must target `io.salezica.chclock.debug`.

## Key constraints (not obvious from code alone)

- Samsung AOD is `Settings.System` key `aod_mode`, not AOSP's `doze_always_on` secure setting.
- Samsung rejects third-party writes to AOD *style* keys (`aod_tap_to_show_mode` etc.) even with elevated permissions. User must set style to "Always" manually; app only detects and warns (`AmbientSamsung.getStyle()` heuristic).
- `WRITE_SETTINGS` is an appop, not a runtime permission: check `Settings.System.canWrite()`, grant via `ACTION_MANAGE_WRITE_SETTINGS` screen.
- AOD style Fix action: no stable public intent for Samsung AOD settings. `AmbientSamsung.openStyleSettings()` tries a candidate chain (aodservice exported `*Setting*` activities resolved at runtime, then hidden action `android.settings.LOCK_SCREEN_SETTINGS`); if nothing resolves, the Fix button is hidden. Needs the `<queries>` manifest block for package visibility.
- No `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — Play policy risk. Fix button opens the system exemption list instead (`PowerUtils.openBatteryOptimizationSettings`).
- FGS type is `specialUse`; manifest carries `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` justification.
- Settings write failures in the service are swallowed deliberately — a crash would loop the sticky service.

## Development aids

- `bin/chclock capture <state>` — mocked dashboard screenshots on any adb device (emulator can't run in the container; use host emulator or a test device — mock touches no real settings).
- `AmbientProvider.DEBUG = true` swaps in `AmbientMock` for tile/service work on non-Samsung devices.
- `MainActivity.startWatchingSettingChanges()` (commented out) + `SettingsReader` log any system-settings change every second — used to discover Samsung's AOD keys.
