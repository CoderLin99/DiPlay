# Steering button identification

Open **Settings → Identify steering buttons**. Enter the vehicle model and model year; the head unit model starts from Android's model and can be corrected. Choose play/pause, next track, previous track or Siri, and press that steering button. G636, FX11 and KX11 use the head unit's OneOS button service directly. Other head units may ask for button access. A complete event fills its mapping automatically. Identification expires after 25 seconds and can be cancelled.

**Save on this head unit** saves an atomic local JSON profile and reloads the active mapping. No upload is scheduled. One to four actions can be configured. Profiles are named `vehicle_head-unit_firmware.json`; unsafe filename characters are replaced and each segment is limited to 64 UTF-8 bytes. These names describe the actual vehicle and head unit; key values do not identify a vehicle model.

Identifying an already assigned input moves it to the newly chosen operation; all changes apply after Save. **Restore original controls** disables the local custom profile immediately while retaining the saved configuration for export. Saving again enables it; reconnecting alone does not.

**Export saved configuration** uses Android's file picker and exports the saved profile selected when the picker opens. Unsaved edits are excluded. This export snapshot survives activity recreation while the picker is open.

## Independent system input

On G636, FX11 and KX11, DiPlay first reads supported steering events directly from OneOS and keeps that input active for saved OneOS profiles. This path does not require log access or local ADB. On other head units, DiPlay runs its own logcat process and owns its broadcast receivers. Learning temporarily suppresses CarPlay commands; stopping it invalidates queued observations from the old monitor.

- Only newly emitted log records are processed; historical records and DiPlay's own diagnostic tags are excluded. A failed log stream is reopened. The app does not clear the system log buffer.
- Numeric or hexadecimal `raw`, `rawKeyCode`, `key`, `keyCode`, `key_code`, `keyId`, `key_id`, `keyValue` and `key_value` fields are recognized. `onKeyDown` and `onKeyUp` messages and named Android `KEYCODE_*` values are supported. Binder transaction `code=` fields are not interpreted as keys. Runtime numeric rules require the saved tag, key and trigger to match.
- A key-related Intent action found in the logs registers an exact broadcast receiver. A broadcast may carry `Intent.EXTRA_KEY_EVENT` or a numeric key extra. The action and extra field names are saved, so subsequent sessions can subscribe directly. A first broadcast discovered only after dispatch requires another press. At most 16 actions are registered per monitor.
- Repeat-count events are ignored; pulse broadcasts and repeated matching log records are debounced. Custom numeric mappings consume the corresponding Android media and focused voice events inside DiPlay to avoid applying the default command again.
- Log mapping cannot cancel another system application's handling of the original physical button. A head unit must actually emit useful log records or expose receivable broadcasts. Buffered, unavailable or protected vehicle events cannot be inferred from the APK alone.

The supplied Application Manager 1.9.0 APK was inspected as a static reference. Its log mapping reads a log stream with elevated access, saves stable log fragments for press/release rules, and handles repeated triggers. DiPlay implements those concepts with its own code and existing Android APIs; no proprietary code or assets are included.

## Access and diagnostics

Declaring `android.permission.READ_LOGS` does not grant cross-process log access. **Allow button access** uses DiPlay's existing local ADB client, asks the head unit to trust DiPlay if needed, and grants only this log permission. The head unit must expose its local network ADB connection. It does not enable ADB, alter system logging levels or install another helper.

An already approved connection may also run:

```sh
adb shell pm grant <installed-DiPlay-package> android.permission.READ_LOGS
```

Seven taps on **Settings → About → Version** unlock the diagnostic page. The diagnostic entry requires both an explicit developer launch and the saved unlock flag; the activity is not exported. Normal settings show assignment and cloud status even after unlocking. Raw keys, event values, log tags, broadcast details and current key-related log snippets are limited to the diagnostic entry.

For a vehicle log without a structured key/event, **Match log fragments** can save an explicit tag and one to four stable literal fragments separated by `&&`. All fragments must occur in the same record to trigger one action. Avoid changing timestamps, process ids and private data. This rule has `keyCode: 0` to explicitly indicate that the log exposes no identified numeric key; it does not invent a key value.

## Profile format

Schema version `1`, backend `system_key_events`:

| Field | Meaning |
| --- | --- |
| `carModel`, `headUnitModel` | User supplied vehicle and head unit names |
| `manufacturer`, `firmware`, `androidVersion` | Android model information, excluding device serial numbers |
| `savedAt` | Save time in milliseconds |
| `bindings` | One to four unique operations and input signatures |
| `operation` | `play_pause`, `next`, `previous` or `siri` |
| `keyCode`, `event` | Observed key and trigger: down `0`, up `1`, single `2`, long `3`, double `4` |
| `source`, `logTag` | `oneos`, `logcat` with an exact tag, or `broadcast` |
| `broadcastAction`, `keyExtra`, `eventExtra` | Observed broadcast action and extra names; empty for log rules |
| `logContains` | Empty for numeric/broadcast rules; stable literal fragments for an explicit log rule |

No real vehicle profile is shipped without actual observed keys. Saved configurations contain model information and mapping rules, not full log streams, cloud account credentials or serial numbers.

## Sharing configuration

Profiles stay on this head unit. Export the saved configuration and attach it to a reviewed issue in [CoderLin99/DiPlay](https://github.com/CoderLin99/DiPlay/issues). This fork has no background profile uploader or third-party cloud endpoint.
