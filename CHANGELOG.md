# CoderLin99 0.2.12-geely.1 — 2026-10-05

- Sync official 2fc876e and Geely 8f53b27, including wireless startup recovery, existing-Wi-Fi connections, resolution/display controls, software Opus decoding and navigation output-device selection.
- Retain Bluetooth compatibility mode, opt-in one-shot handoff, Geely media-stream ownership, duplicate-key filtering, selectable offline diagnostic exports and persistent secondary-display probes.
- Preserve full diagnostics without a developer unlock. This source sync does not publish an APK or claim full-map projection on the 2023 Boyue L. See [integration notes](docs/FORK-SYNC-0.2.12.md).
- Use versionCode 34 and retain the existing signing inputs for future upgrades.

## CoderLin99 0.2.11-geely.4 (vehicle verification build)

- Package the upstream main sync through 6148bc1, including multi-window support, DiLink virtual map cards and optional USB permission auto-confirm.
- Retain geely.3 media-key ownership, destination-picker report export and persisted display probes. Full Geely instrument maps remain unimplemented.
- Use versionCode 33 and the existing signing key for an in-place upgrade from earlier CoderLin99 builds.

## Upstream main sync — 2026-10-04

- Merge shihabal3amri/DiPlay main through 6148bc1 (PRs #170, #171 and #172): optional USB permission auto-confirm, DiLink 3.0/3.5 virtual map cards, and multi-window layout/resolution support.
- Preserve Geely audio/key ownership, the single destination-picker diagnostic export, persisted display probes, factory fullscreen compatibility and settings safe-area padding.
- These source changes are newer than the existing geely.3 APK; this sync does not publish a new release or claim Geely instrument-map support.

## CoderLin99 0.2.11-geely.3 (vehicle verification build)

- On Geely, wait for an actual CarPlay media stream before registering media controls or forwarding media keys. Video-only sessions leave factory Bluetooth controls alone; preserve controls through a pause after CarPlay audio has been established.
- Merge diagnostics into one save action with an optional problem description. Always open the destination picker, including Android 11; if unavailable, explicitly offer default storage instead of silently changing the destination.
- Use an Android 11 window context for the temporary display marker, show the request result in settings, and retain bounded timestamped request/attachment/draw/removal records across process restarts. A drawn window still does not prove visibility on the instrument panel; full instrument maps remain unimplemented.
- Use versionCode 32 and the existing signing key for in-place upgrades.

## CoderLin99 0.2.11-geely.2 (vehicle verification build)

- Preserve Bluetooth music by default. Factory A2DP handoff is opt-in, requires a phone request and runs at most once per session; it never overrides a later manual reconnection.
- Add explicit original-Bluetooth audio mode, retaining CarPlay video while leaving media controls with the original audio source. Reconnect after changing modes.
- Establish a media session when CarPlay connects, before its first music packet, without taking audio focus early. Normalize standard OneOS key aliases, suppress repeated presses and retain raw key / forwarding diagnostics. OEM key consumption still requires vehicle verification.
- Add a five-second secondary-display marker and correct selection of displays sharing the same name. Full Geely instrument maps remain unimplemented pending display-region and permission verification.
- Make offline TXT feedback and sharing the primary report path; GitHub is optional. Add audio negotiation, mode ownership and head-unit recording-route diagnostics. The reported WeChat silence while connected to factory Bluetooth remains under investigation; the app cannot observe WeChat's iPhone recording route.
- Use versionCode 31 and the existing CoderLin99 signing key for an in-place upgrade from geely.1.

- Save diagnostic reports privately in DiPlay when the head unit has no document picker or working Downloads provider, with explicit Share and selectable View report actions. This unblocks collecting logs for #135; its CarPlay startup failure still needs a device report.
- Restore wireless CarPlay sound on KX11 head units while keeping the proven wired audio path.
- Recognize the hotspot interface used by Android 9 KX11 head units so wireless CarPlay can leave the preparation screen.
- Leave direct steering-button compatibility off by default on Android 9 KX11 head units to avoid playback-control conflicts; it remains available in settings.


# Unreleased

Add changes after 0.2.12 here.

- Add automatic wireless setup that uses an active car hotspot when available and otherwise prepares a compatible connection with the correct network name and password.
- Use the car's current hotspot password on Android 9 when the system makes it available, so wireless CarPlay does not keep retrying with outdated saved details.
- Prefer the hotspot's IPv4 connection on older Geely head units and recognize steering-button press and release events on more Geely models.
- Let steering-button identification learn the wider range of key codes reported by OneOS head units.

# DiPlay 0.2.12 — 2026-10-04

- Add Existing Wi-Fi / Same LAN wireless CarPlay with scoped IPv4/IPv6 discovery and network-change cleanup (#223).
- Wait for a stable car-hotspot interface and recover bounded wireless attempts when no AirPlay TCP follows StartSession (#229); add observed-state, authorized-ADB hotspot fallback on firmware exposing supported commands (#235).
- Improve Apple USB attach matching and narrowly scoped optional USB-prompt assistance (#170, #224).
- Pause Android 10 station scans during eligible hotspot/P2P sessions, preserving Same LAN, with controller leases and durable retryable restoration (#225).
- Improve split-screen, launcher cards, short-screen preparation and virtual cluster/floating-map geometry (#171, #172, #181).
- Add independent system-bar controls and correct in-session save/cancel and Local/USB-CH341 authentication selection (#191, #194).
- Add system, light-sensor, day and night CarPlay appearance modes, richer custom turn cards, and live main-video picture controls (#178, #193, #211).
- Offer custom integer resolution from 30% to 160%, with shared limits, correct 30%/160% labels and decoder/canvas capability fallback; refresh connection settings on resume (#179, #230, #196).
- Reconcile opt-in DiLink 4 cluster routing/calibration into one decoder owner, retain verified HUD gates, and journal exact stock-map holds and recovery (#213, #187).
- Add DiLink 3 guidance text and projection-display support with committed recovery before mutation, partial-setup compensation and retryable stock restoration (#182).
- Add opt-in wheel map zoom and main-screen joystick while preserving press/release and call behavior; reject stale queued work across phone/screen changes (#214, #231).
- Switch supported dashboard contents live using actual delivery and safely retained paused choices; preserve selection across stream/phone replacement (#232).
- Add a five-second dashboard-song-on-change window with timer invalidation, and retain album art while the next transfer is pending (#215, #228).
- Export reports through Downloads, document picker, app-external or private fallback storage, with explicit View/Share actions (#185, #219).

See [0.2.12 release notes](docs/RELEASE-NOTES-0.2.12.md) for the complete corrections, hardware evidence and issue-reporting steps. This remains a public preview; no fresh end-to-end vehicle test of the complete repaired release is claimed.

# DiPlay 0.2.11 — 2026-10-03

- Add preferred Wi-Fi Direct channel selection for the next connection; Auto remains the default, and manual channel rejection/mismatch reports an error (#175).
- Add a movable custom dashboard turn card with 2% position steps; leave unknown arrows blank and clear expired guidance (#155).
- Offer two-, three- or four-finger settings swipes, keeping three as the default (#156).
- Add opt-in read-only legacy vehicle-data detection while preserving default DiLink 5.0 mode; reject unaccepted/stale probe publication and fix accepted-battery lock ordering (#158, integrated through #173).
- Keep wireless location/vehicle data on its runtime Wi-Fi link and defer parked-video decisions until SETUP/event readiness (#157).
- Add optional automatic car-hotspot startup, off by default, with verified own-package authorization and unified vehicle settings (#164/#173).
- Support Android TV/remote controls while preserving head-unit touchscreen Back and absolute knob X/Y; keep its workflow source-only (#146).
- Retain artists across partial title updates and publish song metadata/artwork only when changed (#161, #162).
- Correct Android 9 audio API use, release failed codecs, reconnect with settled geometry/readiness, and require own-app VPN scope (#168 and local corrections).
- Add one guarded Auto-mode API 29 P2P recovery for the exact reported pre-create Builder error, plus bounded wireless/media/theme/own-app-exit diagnostics without payload recording or automatic uploads.

See [0.2.11 release notes](docs/RELEASE-NOTES-0.2.11.md) for requirements, device-test limits and fresh-report guidance. Preferred channel selection does not establish stutter as fixed; Qin Plus, Siri, iOS 15 and day/night reports remain under investigation. Android 9 is still the minimum; Wi-Fi Direct needs Android 10+.

# DiPlay 0.2.10 — 2026-10-03

- Publish CarPlay song metadata, position and artwork to Android media sessions; bound artwork queues and reject stale work across sessions (#82).
- Preserve normal USBMUX frames while handling narrowly validated handshake padding (#114); let USB connect without saved wireless-hotspot credentials (#130).
- Handle unknown reported Wi-Fi Direct security types, retry busy channels and allow bounded 5 GHz fallback (#121).
- Select an available AirPlay port and advertise it over Bonjour and wired/wireless iAP2; close sockets on failed setup/notification (#143).
- Enable available platform echo cancellation and noise suppression for calls, restoring the previous mode afterward (#116).
- Detect BYD CAN/CANFD battery protocols and clear unsupported/stale readings (#123).
- Add a saved show/hide setting for the home-screen dashboard-map mirror (#133).
- Improve optional parked video with seeking and ten-second skip controls; validate media URLs and redirects (#129).
- Extend Ukrainian translations, including the new map-mirror setting (#128 and release localization).
- Add bounded anonymous Bluetooth/USB/boot and microphone capture/encode/send diagnostics to exported reports; omit audio and packet contents.

See [0.2.10 release notes](docs/RELEASE-NOTES-0.2.10.md) for contributor credits, requirements and validation limits. Android 9 remains the minimum supported version.

# DiPlay 0.2.9 — 2026-10-02

- Follow BYD head-unit day/night changes while CarPlay is visible, including firmware that does not reliably deliver Android configuration callbacks.
- Restore media and navigation audio stream selection to 0–20 and inherit older saved navigation settings when no new selection exists. Vendor-specific outputs depend on head-unit support.
- Keep CarPlay connected through normal surround-view window changes, preserving video proportions and touch alignment. A connection started in a narrow camera window reconnects once when the window grows to restore the full-screen canvas.
- Add Ukrainian to the app language picker, Android app-language settings, and website. Correct its audio help to describe streams 1–20.
- Add an optional CarPlay song title, artist, and play/pause display on the BYD instrument cluster, using the existing network ADB connection.
- Add a CarPlay navigation widget for launchers that host standard Android widgets, with the next turn, road, distance, arrival information, and song. Clear expired guidance and explicitly cleared song titles.
- Add an optional floating copy of the dashboard map on the centre screen, with drag, pinch-to-resize, and tap-to-open controls. Requires permission to draw over other apps; Usage Access restricts it to home screens.
- Fix floating-map resizing on head units that ignore small pinch gestures.
- Let compatible launchers embed the live dashboard map on Android 11 and newer. Sharing is off by default; turning it off closes existing shared map views.
- Add map-host and DiPlay Home sample apps for developers. DiPlay Home combines the live map, standard Android widgets, a clock, and an app list; sample builds, lint, and Home back-navigation tests are checked in CI.
- Leave the GPS course empty when its direction is unknown, instead of reporting north. Valid GPS directions are preserved.
- Thanks to @lpcheng1208 for PRs [#71](https://github.com/shihabal3amri/DiPlay/pull/71), [#88](https://github.com/shihabal3amri/DiPlay/pull/88), and [#89](https://github.com/shihabal3amri/DiPlay/pull/89).
- Thanks to @romanchukg-cloud for PRs [#93](https://github.com/shihabal3amri/DiPlay/pull/93), [#101](https://github.com/shihabal3amri/DiPlay/pull/101), [#105](https://github.com/shihabal3amri/DiPlay/pull/105), [#106](https://github.com/shihabal3amri/DiPlay/pull/106), [#107](https://github.com/shihabal3amri/DiPlay/pull/107), [#108](https://github.com/shihabal3amri/DiPlay/pull/108), and [#109](https://github.com/shihabal3amri/DiPlay/pull/109).

See [0.2.9 release notes](docs/RELEASE-NOTES-0.2.9.md) for the merged changes and validation limits.

# DiPlay 0.2.8 — 2026-09-30

- Keep iPhone location reporting active across the wireless Bluetooth-to-Wi-Fi CarPlay handoff; limit location updates to one per second on wireless and USB.
- Add optional ADB wheel-speed and gear reporting for iPhone dead reckoning when GPS is unavailable. Tunnel use has not yet been verified.
- Add optional iOS 27 video playback on the car screen while parked, with iPhone, touchscreen and steering-wheel controls; close playback when leaving P.
- Explain unsupported DRM-protected video such as Apple TV+, which requires a licensed FairPlay receiver.
- Improve playback error reporting and preserve CarPlay when the head unit cannot play a video.

# DiPlay 0.2.7 — 2026-09-29

- App interface in English, Simplified Chinese, Arabic, Russian and Spanish; synchronized Android app-language settings.
- Steering-wheel media controls and long-press Siri on supported BYD firmware while CarPlay is on screen.
- Dashboard display choices: map, turn card, or both; corrected dashboard keyframe recovery.
- Optional ADB feature on supported DiLink 5.0: pause the dashboard map stream when its display mode hides the map.
- Optional ADB battery reporting for Apple Maps, with warning threshold, charging-connector selection and a checked reconnect action.
- Audio playback reliability fixes and clearer dashboard settings.
- Clarify the BYD-only support scope on the README and all five website editions.

# 0.2.0 — BYD navigation and connection improvements

- Standalone windshield HUD arrows, distance and street names on the verified DiLink5.1 firmware; no ADB, root or computer helper.
- Retain contributor cluster/SOME-IP navigation, route parsing, BYD CarPlay icon and display-size presets.
- Fix Car hotspot startup by using scoped IPv6 when available and binding discovery/probing to the AP interface. Physically confirmed on the development car.
- Drain asynchronously decoded audio during packet gaps and rebuild the music buffer after starvation. Wi-Fi Direct is much better in the user retest; occasional audio cutouts remain for a later version.
- Preserve bounded music-buffer choices, USB read improvements and decoder recovery; fix USB request/close races and keep vendor output outside phone callbacks.
- Save audio/video/receive timing and discovery diagnostics without road names or protocol payloads.
- HUD cleanup on normal end/disconnect/off/stale input; interrupted sessions recover on the next app launch. Force-stop may leave guidance visible until reopening.
- Thanks to @romanchukg-cloud and @georgiyrr for PR #3 and vehicle testing.

# 0.1.0 release restored — 2026-09-25

- Rebuilt and signed the APK locally with explicitly supplied runtime authentication assets.
- Restored release downloads; no app behavior or version-code change from 0.1.0.
- Accessory identity remains in the APK only. No credential files enter Git or the source archive.
- Retained generated test identities and public-source credential checks.
- Source/CI builds omit runtime identity assets by default; local packaging requires an explicit external directory.

# Source reset — 2026-09-25

- Withdrew the 0.1.0 APK and removed its release tag.
- Reset the public branch after preserving restricted local incident records.
- Removed static synthetic test private keys; generate test identities at runtime.
- Removed automatic private-asset packaging and disabled the old release build script.
- Added a build guard rejecting credential asset files.
- Replaced the download site with a five-language suspension notice.

The APK was subsequently rebuilt and restored as described above. Existing copies cannot be recalled by a Git history reset.
