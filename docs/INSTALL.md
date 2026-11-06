# Install and try Beam Remote

## Requirements

- Android 6.0 / API 23 or newer.
- For real appliance control, a built-in IR emitter accessible through Android's `ConsumerIrManager`.
- Clear line of sight from the phone's IR window to the appliance.

Phones without an accessible emitter and emulators can use Preview. Bluetooth, Wi-Fi, RF, and USB IR accessories are not implemented.

## Install an APK

1. Open the repository's **Releases** page and download the APK from a release. Initial releases are development builds for testing.
2. Transfer the APK to the phone and open it with Files.
3. If Android asks, allow that file manager to install apps from this source, then return and install. Menu labels vary by manufacturer.
4. Open **Beam Remote → Help** and check whether an IR emitter is detected.

Keep exported copies of important profiles. Updating an APK signed with the same key should retain app data; uninstalling or clearing storage removes it. A locally built debug APK may have a different signature from the release asset and may not install as an update. Do not uninstall to resolve that without exporting your profiles first.

Developers can install a local build over USB after enabling USB debugging and authorizing the computer:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## First test

1. Keep Preview on. Add a device and select a code family matching the original remote model.
2. Try a command; the result should explicitly say that nothing was transmitted.
3. To test hardware, turn Preview off and point the phone at the appliance from about 0.5–1 m away.
4. Send one command, then observe the appliance. The app cannot confirm reception.

For a Whirlpool AC, turn the AC on with its original remote before testing a temperature change. In v1.2.0 each button sends immediately; no Apply step is needed. **Power on/off** is a toggle; send it once and observe before tapping again. Candidate families are DG11J1-3A/04 and DG11J1-91. A user has reported basic operation for DG11J1-35 using the 3A/04 profile; individual functions and new extras still need verification.

For a Voltas AC, add **Voltas AC · 183V DZU2 · protocol candidate**. This profile implements a 38 kHz 183V DZU2 mapping. Check the full model suffix on your indoor unit or remote first: R32 identifies the refrigerant and does not guarantee this remote protocol. Try one command at a time in Preview, then test at short range with the AC in view. The mapping has not been confirmed on your exact R32 unit.

For detailed device verification and troubleshooting, see [TESTING.md](TESTING.md). To add custom scenes or exchange profiles, see [PROFILE_FORMAT.md](PROFILE_FORMAT.md) and the JSON files in `examples/`.

## Add an offline profile library

On Devices, choose **Import profile library**, select your `.beamzip` file, search the brand and test a numbered candidate. **All commands** contains the extra functions available in that candidate. Scroll to **Save this remote** after checking the required controls on your AC. The app does not automatically scan candidates or confirm reception. [Full library guide](PROFILE_LIBRARY.md).
