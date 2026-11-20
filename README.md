# TV Button Remap

A tiny Android TV accessibility service for the Sony BRAVIA remote:

- **Settings** (gear) button opens **All apps** in
  [Hearth](https://github.com/rusty-workshop/hearth), but only while Hearth's
  home screen is in front. Pressing it again while All apps is open goes back
  to the home screen. In any other app it opens Sony's settings as usual.
  (Before Hearth, this opened Projectivy's settings.)

The gear button sends `KEYCODE_BUTTON_2` (189), which normally goes to Sony's
quick-settings receiver. Accessibility key filtering sees keys before Sony's
global-key handling, so the service can consume it and open something else.

The YouTube button (`KEYCODE_BUTTON_9`, 196) used to open SmartTube the same
way. That remap was dropped in 1.6, and the button opens YouTube as normal.

It also shows **pop-ups** (bottom-right card, 12 s, never takes focus) for
anything that sends:

```bash
adb -s 192.168.1.75:5555 shell am broadcast -n com.rusty.tvremap/.MessageReceiver \
  --es title "'Ready to watch'" --es text "'Some Movie (2003)'"
```

The receiver requires `android.permission.DUMP`, which adb's shell holds
and normal apps can't, so only adb can trigger it. The window is an
accessibility overlay, so it needs no "display over other apps" permission.
My Jellyfin setup uses it for new-movie
announcements.

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
Hearth's Home-button service). Run this after every install: the system can rewrite the
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
