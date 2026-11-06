# Testing Beam Remote

## Automated checks

Run `./gradlew assembleDebug testDebugUnitTest lintDebug`.

Unit tests verify Samsung power and NEC power wire vectors, LG full-state and off vectors, all 375 combinations of supported AC temperature/mode/fan values with both timing variants, checksums, timing bounds, defensive signal copying, invalid raw input, catalog serialization, custom AC scenes, unknown schema versions, and malformed imports.

Unit tests cannot prove that an OEM IR driver emits accurate timings or that an appliance accepts a candidate code. A debug build and emulator run also cannot prove IR compatibility.

## Emulator / no-emitter checks

1. Launch with clean app data. Devices shows an empty state; Preview defaults on.
2. Add a Samsung TV, press Power. The result must say “Preview only” and “Nothing transmitted”.
3. Add an LG AC. Set 25°C, another mode and fan. Nothing is sent until Apply. Try Apply and Off in Preview.
4. Navigate away, rotate/recreate, force-stop and relaunch: profiles, selected device, preview preference, and AC draft settings should survive. No automatic command should be sent.
5. Edit metadata. Export to a local document, import it, and verify the commands and metadata. Import must add a distinct profile, not overwrite one.
6. Add a raw command; reject zero/negative/non-numeric durations, duplicate names, and a ≥2-second pattern. Delete a command and device and relaunch to check persistence.
7. Turn Preview off on an emulator and try Power. Expect an explicit no-emitter error, never “Signal sent”.
8. Test large fonts, small screen, landscape, screen reader labels, navigation-bar insets, and long command labels. Content must scroll rather than hide controls behind the keyboard/system bars.
9. Cancel file pickers and dialogs. Test invalid JSON, unknown schema version, fractional durations, over-512-KB import, inaccessible file provider, and empty command sets. Existing profiles must remain intact.

## Physical phone and appliance matrix

Use a real Android phone with an IR emitter exposed through `ConsumerIrManager`. Start at roughly 0.5–1 m with clear line of sight. Do not infer emitter failure from a camera test alone; some cameras filter infrared.

| Record | Example / expected observation |
| --- | --- |
| Phone / Android / build | Manufacturer, exact model, Android version and build |
| IR status | Help reports emitter present; note carrier ranges or unknown |
| Appliance | Brand, exact model, and original remote model |
| Profile | Candidate family or raw capture source |
| Command | Power / volume / complete AC state |
| App result | Sent / unsupported frequency / hardware error |
| Appliance result | Actual observed response; do not substitute “sent” |
| Reliability | Responses out of 10 attempts, distance and angle |

TV: test power once, volume up/down, mute, channel, input and navigation. Allow the appliance to settle between power tests; toggles can undo a prior press. Record unsupported buttons separately.

AC: test explicit Off, Cool 24°C Auto, temperatures 16/30, all supported modes and fan levels appropriate to the appliance. Some units have no heating capability even if the protocol can encode it. Confirm full-state replacement after using the original remote. Test LG28 and LG2 only against matching remote families. Do not assume swing or timers work; they are intentionally absent.

For custom AC scenes, capture complete known-good frames using a separate IR receiver (for example an Arduino/ESP receiver). Compare received frequency/timings and decoded state with the original remote. ConsumerIrManager cannot receive or learn signals. If a frame needs repeats, include them with their gaps within the API's duration limit.

## Troubleshooting

- **No emitter:** public Android API unavailable, missing hardware, or OEM-only driver. The app cannot fix that with a permission prompt.
- **Sent, no response:** wrong model/code family, obstructed IR window, wrong orientation/range, carrier mismatch, or missing protocol repeats. “Sent” only means the API call returned.
- **Unsupported carrier:** use a compatible phone or a verified code using a supported carrier; changing the frequency arbitrarily will not repair a code.
- **AC changed unexpectedly:** a raw capture includes full state; label the scene with all settings. UI state is not feedback from the appliance.
- **Corrupt saved data:** the app preserves the existing preference data and blocks overwriting it. Restore an exported profile after clearing storage only if you accept losing the unreadable local data.

## Run record

See `VALIDATION.md` for the checks actually completed for this delivery. Physical phone/appliance tests remain pending until hardware is available.

## Whirlpool acceptance checks

- Add both Whirlpool candidates and confirm temperature limits: 18–32°C for 3A/04, 16–30°C for 91. Verify Cool/Dry/Fan/Heat and Auto/Low/Medium/High fan lists; the separate experimental 6th Sense button.
- In Preview, each settings button and Power show a 38 kHz / 343-duration signal. Each deliberate button tap must send exactly once; opening a screen and restoring settings must never send. Inspect a receiver capture: Settings actions must have the toggle bit clear, Power must set it and use command category 1. A temperature action after Power must clear it again.
- Restart and export/import after selecting Heat, Low fan and a boundary temperature. The model, values and last-command category should survive; no power toggle should be persisted or transmitted automatically.
- On matching physical hardware, test a single Power toggle and confirm response, then tap to change temperature without toggling power. Verify modes/fans and both model variants only on compatible units. Check for required repeat behavior, inter-section timing and carrier compatibility.
- Test with previously enabled timers/special modes: timer flags remain off; the new sleep/swing/Turbo flags reflect desired settings. No claim is made that these advanced settings are preserved.
- Regression: existing LG controls retain explicit Off and On/Apply behavior, and existing TV/raw profiles remain usable.

## Voltas 183V DZU2 protocol candidate

- Confirm the full AC/remote model before treating this as a match. R32 alone does not identify the remote protocol.
- In Preview, check Cool/Dry/Fan/Heat, 17–30°C, Auto/Low/Medium/High, explicit On/Off, Swing, Sleep, Eco, Turbo toggle and Display toggle. Dry forces Auto fan; temperature controls are disabled in Fan-only mode.
- On a compatible physical unit, point the phone at the receiver and test one command at a time from short range. Confirm power, a temperature change, mode and fan response separately. The app receives no appliance acknowledgement.
- Verify that Turbo and Display toggle buttons are not repeated accidentally; these protocol commands toggle the appliance feature state.
- The public protocol report names 183V DZU2 as tested by its author. That report does not certify the user's R32 model or phone transmitter.

## v1.2.0 remote acceptance checks

- Tap TEMP + once: exactly one degree change and one send/preview. Tap TEMP − once to reverse. Boundary buttons are disabled. No long-press repeats.
- Tap Mode/Fan: cycle one setting and send once. No Apply button. Power must not repeat or send while restoring the screen.
- In basic 6th Sense, check 23°C / Auto fan and disabled temperature/fan controls. Mode returns to manual settings.
- Tap Swing/Sleep/Turbo on and off individually. Observe the AC, not just a beep. Verify sleep's low fan and Turbo's high fan/temperature extreme. Repeat after restart and JSON export/import.
- During an active send, another tap must not change desired settings or queue a later power toggle. After completion the next tap works normally.
- Existing saved Whirlpool profiles without new flags load with all extra flags off. LG retains explicit On/Off and sends settings per tap.

## Optional profile libraries

- Import a library through the Android document picker; search brands and open/close multiple candidates. No transmission should occur until a command is deliberately tapped.
- In Preview, verify one temperature step resolves one stored command and respects missing combinations; candidate trials do not enter the saved-device list until Save this remote. Save and restart, then check the same library reference and requested settings.
- Check a relative-only fan/mode candidate as well as full-state candidates; valid relative controls must be enabled even without an absolute speed list.
- All commands must expose stored extra functions; malformed signals report errors before requested state changes. Test the six identified source defects separately from valid commands.
- Import an unrelated library and ensure existing remotes still use their original hash-bound library. An exported device without its original library must show an import-needed error.
- Synthetic tests cover Pronto count validation versus explicit compatibility timing, signal limits, unsafe/duplicate archive paths and library metadata/reference validation. Never treat these checks as appliance certification.
