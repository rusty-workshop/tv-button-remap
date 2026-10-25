# TV Button Remap

A tiny Android TV accessibility service for the Sony BRAVIA remote:

- **YouTube** button opens **SmartTube** instead, everywhere.
- **Settings** (gear) button opens **Projectivy's settings**, but only while
  the Projectivy home screen is in front. In any other app it opens Sony's
  settings as usual.

The YouTube button sends `KEYCODE_BUTTON_9` (196). Android routes it through
the global-key table to Sony's `sonyvendorservice`, which hands it to Google
TV, which opens YouTube. Accessibility key filtering sees the key first, so
this service consumes it and launches SmartTube. If SmartTube isn't
installed, the button falls through to its normal behavior.

The gear button is `KEYCODE_BUTTON_2` (189), normally routed to Sony's
quick-settings receiver. To know which app is in front, the service listens
for window-state changes and only counts real activities, so toasts and the
volume bar don't count as leaving the launcher.

No UI, no network, no window-content access. Add more buttons to the
`remaps` (anywhere) or `launcherRemaps` (home screen only) maps in
`RemapService.kt`. Every key press is logged at debug level under the
`TvButtonRemap` tag, which is the easy way to find a button's key code:

```bash
adb -s 192.168.1.75:5555 logcat -s TvButtonRemap
```

Injecting keys with `adb shell input keyevent` doesn't go through this
service (or Sony's handling), so test with the real remote.

## Build and install

```bash
./gradlew assembleRelease
adb -s 192.168.1.75:5555 install -r app/build/outputs/apk/release/app-release.apk
```

Enable the service (appending to any already-enabled services, such as
Projectivy). Run this after every install: the system can rewrite the
setting while the package is being replaced.

```bash
adb -s 192.168.1.75:5555 shell settings put secure enabled_accessibility_services "$(adb -s 192.168.1.75:5555 shell settings get secure enabled_accessibility_services):com.rusty.tvremap/com.rusty.tvremap.RemapService"
```

## Undo

Turn it off under Settings → Accessibility on the TV, or uninstall it:

```bash
adb -s 192.168.1.75:5555 uninstall com.rusty.tvremap
```

## License

AGPL-3.0-or-later
