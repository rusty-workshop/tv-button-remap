# TV Button Remap

A tiny Android TV accessibility service that makes the Sony BRAVIA remote's
**YouTube** button open **SmartTube** instead.

The YouTube button sends `KEYCODE_BUTTON_9` (196). Android routes it through
the global-key table to Sony's `sonyvendorservice`, which hands it to Google
TV, which opens YouTube. Accessibility key filtering sees the key first, so
this service consumes it and launches SmartTube. If SmartTube isn't
installed, the button falls through to its normal behavior.

No UI, no network, no window-content access. Add more buttons to the
`remaps` map in `RemapService.kt`.

## Build and install

```bash
./gradlew assembleRelease
adb -s 192.168.1.75:5555 install -r app/build/outputs/apk/release/app-release.apk
```

Enable the service (appending to any already-enabled services, such as
Projectivy):

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
