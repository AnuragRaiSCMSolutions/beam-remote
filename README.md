# Beam Remote

An ad-free native Android infrared remote MVP for TVs and air conditioners, built with AI as a development partner. It uses your phone’s built-in IR blaster through Android’s public `ConsumerIrManager` API.

**No ads, accounts, analytics, or internet permission.** Android 6.0 / API 23 or newer. The project is at version **1.4.0** and is licensed under [MIT](LICENSE).

<img src="docs/images/ac-remote.png" alt="Beam Remote AC controls in preview mode" width="280">
<img src="docs/images/tv-remote.png" alt="Beam Remote TV controls in preview mode" width="280">

## Try the app

Download the development APK from [Releases](https://github.com/wstrhwkdnd-afk/beam-remote/releases), then follow the [installation and first-test guide](docs/INSTALL.md). For an Android Studio project, clone the repository or use **Code → Download ZIP**.

A real Android-accessible IR emitter is required for appliance control. Phones without one can explore the interface in Preview. A successfully sent signal does not prove the appliance received it.

## Offline profile library

Version 1.3.0 adds optional multi-brand libraries: **Import profile library → search brand → test a candidate → Save this remote**. A library can contain hundreds of candidates without filling your saved-device list. Each button sends one stored signal; **All commands** exposes extra functions present in that candidate.

A local AcRemote 3.5 interoperability investigation inventoried 86 brands and 266 candidates. That third-party data is separate from the public app/source; the APK does not include or download it. Import a library you can use. [Library guide, format and investigation findings](docs/PROFILE_LIBRARY.md).

## Device coverage

Built-in families are candidates requiring verification on your hardware. A user has reported basic AC operation using the 3A/04 profile with a DG11J1-35 remote family; exact working functions have not been recorded.

| Device | Candidate family | Controls |
| --- | --- | --- |
| Samsung TV | BN59-01180A family | Power, volume, mute, channel, input, navigation, back, menu |
| LG TV | NEC address 04 | TV controls including OK |
| LG AC | LG28 / 6711A20083V; LG2 / AKB75215403 | Power, 16–30°C, mode, fan |
| Whirlpool AC | DG11J1-3A / 04 | 18–32°C, mode, fan, power toggle; experimental extras |
| Whirlpool AC | DG11J1-91 | 16–30°C, mode, fan, power toggle; experimental extras |
| Voltas AC | 183V DZU2 · protocol candidate | 17–30°C, Cool/Dry/Fan/Heat, fan, explicit On/Off, Swing, Sleep, Turbo toggle, Eco, Display toggle |
| Other devices | Custom raw codes / complete AC scenes | User-supplied commands |

**Whirlpool DG11J1-35 has a user report of partial/basic operation with the 3A/04 profile.** This is not full model certification; the new extra functions remain unverified on that hardware. Contributions with receiver captures and physical test results are welcome.

## Open and run

1. Open this folder in a current Android Studio version supporting Android Gradle Plugin **9.4.1**.
2. Install **Android SDK Platform 37.0** and **Build Tools 36.0.0** through SDK Manager. Use Android Studio's bundled JDK (JDK 17+; this project was built with its bundled runtime).
3. Let Gradle sync. The checked-in wrapper downloads **Gradle 9.8.1** and verifies the distribution checksum. Android Studio creates `local.properties` for your SDK path; this file is machine-specific.
4. Select the `app` configuration and your phone, then Run. Enable USB debugging on the phone and accept its debugging prompt.
5. Add a device. Pick a remote-code family, point the phone's IR window at the appliance, and test one command. Check Help to see hardware capabilities and the Preview switch.

Command line, with `JAVA_HOME` set to a compatible JDK and `ANDROID_HOME` or `local.properties` pointing at the SDK:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Windows: use `gradlew.bat`. A debug APK is for testing; production distribution requires your own release signing setup. The pinned Gradle distribution is checksum-verified. With Android command-line tools, the platform package is `platforms;android-37.0`.

[GitHub Actions](https://github.com/wstrhwkdnd-afk/beam-remote/actions/workflows/android.yml) runs the build, JVM tests and lint on pushes and pull requests, and keeps the debug APK and reports as workflow artifacts. Hardware compatibility tests remain separate.

## Included in the MVP

- Multiple named device profiles, stored locally; select, rename, delete, import and export.
- Samsung TV BN59-01180A-family candidate: power, volume, mute, channel, input, navigation, back and menu. No unverified Samsung OK code is supplied; add a captured command if needed.
- LG TV NEC-address-04 candidate, including OK. A code family does not imply every model is supported.
- LG AC LG28 / 6711A20083V and LG2 / AKB75215403 candidates: explicit off, on/apply, 16–30°C, mode and fan. A remote-shaped panel sends one complete-state command per button tap; desired settings persist.
- Whirlpool AC DG11J1-3A/04 (18–32°C) and DG11J1-91 (16–30°C) candidates: Cool/Dry/Fan/Heat, Auto/Low/Medium/High fan, one-tap temperature, Mode and Fan buttons, and a transient Power toggle. Experimental basic 6th Sense/Auto, vertical swing, sleep and Turbo/Jet controls are included. Heat needs a heating-capable unit. Timers and model-specific advanced 6th Sense/sleep programs are not included.
- Voltas AC 183V DZU2 protocol candidate: 17–30°C Cool/Dry/Fan/Heat, Auto/Low/Medium/High fan, explicit On/Off, Swing, Sleep, Turbo toggle, Eco and Display toggle. The source mapping was tested on 183V DZU2; an R32 refrigerant label alone does not identify a matching remote protocol. Timers and Follow Me are not exposed in this UI.
- Custom TV buttons and custom AC **complete-state scenes** using raw microsecond patterns. This provides a practical path for other brands without pretending they share an AC protocol.
- `ConsumerIrManager` transmission off the UI thread, carrier-range checks where the OEM reports them, validation below Android's two-second limit, error reporting, and dropped rapid taps rather than an unbounded command queue.
- Preview mode automatically defaults on when there is no accessible emitter. Preview never transmits; transmission is never reported as appliance confirmation.
- Native scrollable screens, large labeled buttons, system font scaling, system-bar insets, and accessibility announcements for command results.

## Coverage and limits

This is an extensible MVP, **not an exhaustive universal code database**. Presets are protocol/code candidates and have not been verified on your appliances. TV and Whirlpool AC power are toggles; Voltas AC uses an explicit Off frame and a settings frame for On. AC state is only the desired state, not telemetry. No automatic code scanning, learning, Bluetooth/RF/Wi-Fi control, macros, press-and-hold repetition, AC timers, or cloud database is included. LG swing is not implemented. The UI is English-only.

The phone model is not needed to build or browse the app: it probes Android's public IR API at runtime. A physical emitter hidden behind a vendor-only API will appear unavailable. If a command fails on real hardware, record the phone model, Android version, appliance model, and original remote model using the testing checklist.

Profiles are local app data; uninstalling or clearing storage removes them. Export important profiles first. Device transfers may be affected by Android/OEM backup policy. Imports create new profile IDs and never overwrite another device. File transfers are bounded at 512 KB; 50 devices and 64 commands per device are supported. Raw signals are limited to 4,096 durations and less than two seconds.

If an import is interrupted by activity destruction while a slow document provider is reading, retry the import; existing profiles are preserved. The app deliberately avoids background signal queues and automatic retries because repeated power commands can undo the intended action.

## Project map

- `MainActivity.java`: native screens, profile editing and Android document picker.
- `Profile.java`, `ProfileStore.java`, `Catalog.java`: schema validation, local persistence and candidate families.
- `Signal.java`, `Encoders.java`, `WhirlpoolEncoder.java`, `VoltasEncoder.java`: bounded raw signals and Samsung/NEC/LG/Whirlpool/Voltas wire formats.
- `IrTransmitter.java`: public Android IR API adapter and serialized transmission.
- `app/src/test/`: protocol vectors, boundary validation, and profile round-trip tests.
- [Hardware and test checklist](docs/TESTING.md)
- [Protocol references and extension guide](docs/PROTOCOLS.md)
- [Profile format](docs/PROFILE_FORMAT.md)

Android's [ConsumerIrManager documentation](https://developer.android.com/reference/android/hardware/ConsumerIrManager) defines the transmit API and its limits. Protocol-specific research links are recorded in the protocol guide.

## Remote-style controls (v1.2.0)

Update over the existing APK to retain profiles. Select the existing Whirlpool 3A/04 profile if that is working for your AC. Every button tap sends immediately (or previews one command when Preview is enabled). There is no Apply step.

- **Power on / off:** sends one transient toggle. Observe the AC before tapping again; power is not tracked as known state.
- **TEMP − / +:** changes one degree and sends a complete settings frame without toggling Whirlpool power. Temperature limits disable further changes.
- **Mode / Fan speed:** cycles to the next setting and sends it. Mode cycles Cool → Dry → Fan → Heat; heating requires a compatible AC.
- **6th Sense:** experimental basic Auto mode at 23°C and Auto fan. Press Mode to return to manual control. It does not implement proprietary advanced programs.
- **Swing / Sleep / Turbo:** tap to change the desired setting and transmit. Sleep selects low fan. Turbo selects high fan and the minimum cooling temperature (maximum in Heat). Temperature, mode and fan changes cancel Turbo; selecting a non-low fan cancels Sleep.

Buttons do not send on opening a profile, restarting or importing. Taps while an earlier signal is still being sent are refused before changing settings; wait for completion. AC protocols carry full state, so these are individual button actions rather than standalone temperature-up/down IR codes. The display shows requested settings, not appliance feedback. Power cancels Turbo. Preview actions also update the saved desired settings.

The full frames carry timer flags off and clock 00:00. Existing timer programs can therefore be canceled. Importable examples remain in `examples/`.

## Contribute

See [CONTRIBUTING.md](CONTRIBUTING.md) for development and device-profile contributions. When reporting compatibility, include the phone/Android version, appliance model, original remote model, chosen profile and actual observed response. Avoid publishing phone serial numbers or private exported profiles.

## License and acknowledgments

Original code and documentation: [MIT License](LICENSE). Gradle wrapper assets retain their Apache 2.0 license. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for build/test dependencies and protocol-research references.

This is a personal community project, with no affiliation to the appliance or phone manufacturers named above.
