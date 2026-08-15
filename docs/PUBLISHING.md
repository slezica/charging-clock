# Publishing to the Play Store

What changed on this branch, what needs care during Play Console setup, and
where rejection risk lives.

## Changes vs `master`

- Removed `AmbientDefault` (AOSP `doze_always_on` backend) and the
  `WRITE_SECURE_SETTINGS` permission. That permission can only be granted over
  ADB, which made Play distribution pointless for stock Android. One UI only now.
- Removed `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Play policy restricts it to
  apps whose core function breaks without it, and review is strict. The status
  row's Fix button now opens the system exemption list
  (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`, no permission needed) and the
  user picks the app manually.
- Added release signing config, loaded from `keystore.properties` (untracked).

## Remaining permissions

| Permission | Status |
|---|---|
| `WRITE_SETTINGS` | User-grantable appop, in-app grant screen. Not Play-restricted. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Needs Play Console declaration. See below. |

## Care item 1: foreground service `specialUse` declaration

This is the main review gate. Since targetSdk 34, every foreground service
needs a typed declaration in the Play Console, and no enumerated type
(mediaPlayback, location, health, ...) fits "watch charger, flip a setting".
`specialUse` is the escape hatch, and Google reviews it by hand.

The reviewer reads the manifest property
(`PROPERTY_SPECIAL_USE_FGS_SUBTYPE` in `AndroidManifest.xml`) and the console
form. The key question they will ask: **why a foreground service at all?**

The honest, correct answer — use it verbatim in the declaration:

> The app toggles Samsung's Always-On Display when the charger connects or
> disconnects. `ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED` cannot be
> received by manifest-declared receivers since API 26; they are only delivered
> to receivers registered by a live process. A foreground service is the only
> supported way to keep such a receiver alive. No enumerated FGS type covers
> this use case. The service does no data collection and no network access.

A short screen-recording (plug in → AOD turns on → unplug → AOD off) attached
to the declaration helps. Rejection here is not fatal: reword and resubmit.

## Care item 2: battery optimization UX regression

Dropping `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` traded a one-tap dialog for a
manual flow (open list, find app, choose "Don't optimize"). The hint text in
the app explains the steps. Do not "fix" this by re-adding the permission
without reading the current
[Play policy on exemptions](https://developer.android.com/training/monitoring-device-state/doze-standby#support_for_other_use_cases) —
it is a common rejection cause.

Note the app mostly acts *while plugged in*, when Doze is largely inactive, so
the exemption matters less than it looks. The service being killed while
unplugged only delays the "AOD off" write until the next event.

## Care item 3: device targeting

There is no manifest `<uses-feature>` for "Samsung One UI with AOD". The app
installs on any device; on non-Samsung it shows "Not detected" and does
nothing.

- State "Samsung One UI devices only" prominently in the listing description.
  Expect some 1-star "doesn't work" reviews from non-Samsung installs anyway.
- Optionally use Play Console → Device catalog to exclude non-Samsung devices.
  Crude (maintained by hand, catalog churns), but reduces mis-installs.

## Care item 4: listing content

- Description must mention the AOD style limitation: the app toggles the AOD
  master switch, but the style must be set to "Always" manually in Samsung
  settings (Samsung blocks third-party writes to the style keys — see comment
  in `AmbientSamsung.java`).
- Data safety form: no data collected, no data shared, no network access. The
  app has no `INTERNET` permission, which makes this section easy.
- App category: Tools.

## Care item 5: signing and release builds

1. Create a keystore (once, back it up — losing it with Play App Signing is
   recoverable, without it is not). Path and alias are fixed conventions read
   by `app/build.gradle` and `bin/chclock`:

   ```
   $ keytool -genkeypair -v -keystore secrets/release.keystore \
       -keyalg RSA -keysize 2048 -validity 10000 -alias chclock
   ```

   `secrets/` is gitignored. The password is never stored on disk:
   `bin/chclock prepare` prompts for it and passes it to Gradle via the
   `KEYSTORE_PASSWORD` environment variable (host memory only).

2. Prepare the release (host only — interactive password prompt):

   ```
   $ bin/chclock prepare --version X.Y.Z --changes "..." [--screenshots]
   ```

   Bumps versions, updates `docs/VERSIONS.md`, renders the 512px listing icon,
   builds signed APK + AAB into `dist/`, commits and tags `vX.Y.Z`.
   Play requires the App Bundle: upload `dist/chclock-release-X.Y.Z.aab`.

3. Enroll in Play App Signing on first upload (mandatory for new apps). The
   local keystore becomes the *upload* key.

## Care item 6: per-release chores

- `bin/chclock prepare` bumps `versionName`/`versionCode` (scheme
  `X*10000 + Y*100 + Z`); Play rejects duplicate `versionCode`.
- Keep `targetSdkVersion` within Play's yearly deadline (currently 35, fine).
- Each release with an FGS goes through the declaration review again; keep the
  `specialUse` justification stable between releases.

## Known risks, ranked

1. **FGS `specialUse` review** — likely approved with the justification above,
   but human review, so variance. Iterate on rejection.
2. **Non-Samsung installs** — support noise, bad reviews. Mitigate via listing
   text and device catalog.
3. **Samsung changing `aod_mode` semantics** in a future One UI — would break
   silently. The status dashboard surfaces mismatches, but there's no
   proactive detection.
