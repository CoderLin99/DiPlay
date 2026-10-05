> CoderLin99 integration: this document describes the imported carlito changes. Its cloud-upload behavior has been replaced with local export and GitHub issue submission; see [fork release notes](FORK-RELEASE-0.2.11-GEELY.1.md).

# Geely G636 / FX11 / KX11 factory CarPlay adaptation

This adaptation targets the Geely factory CarPlay implementation supplied for G636, FX11 and KX11 head units. It follows the upstream application version, currently **0.2.11 / code 30**. A Xingyue L KX11 user report confirms the Android identity and exposes the Bluetooth handoff and full-screen failures; the Bluetooth handoff fix is driven by the iPhone request and also applies to other affected head units. The resulting changes still need an installed vehicle retest.

## Reference evidence

The supplied `com.autolink.carplay.apk` provides the receiver service; `com.autolink.carplay.app.apk` provides its interface and car integration. Reference APKs and decompiled files remain outside this repository.

| Area | Factory source evidence | DiPlay behavior |
| --- | --- | --- |
| Configuration | `ConfigManager` reads `/vendor/etc/carplay/carplay_config.json`. The APK asset is a template, not the configuration read by this manager. | Read the installed vendor configuration. Retain only manufacturer, artwork and audio routing sections. |
| Desktop icon | `OemIconInfo` names 104, 120, 180 and 256 pixel PNGs under `/vendor/etc/carplay/`, labelled Geely. The PNGs are absent from the supplied APKs. | Use readable installed PNGs at their original sizes. A valid custom image takes priority; interrupted saves retain the last complete image. If no factory artwork is readable, use an authored house vector, rendered to a 256 pixel PNG at runtime. The main settings screen exposes image selection, square cropping, preview and restore-default controls. |
| Desktop return | `CarPlayDisplayActivity.requestVideoFocus` moves the projection task to the background. | The existing CarPlay home request opens the default Android home screen and retains the background session. Reject home requests from an obsolete session. |
| Audio focus | `AudioAdapter` requests persistent media focus and transient navigation, Siri, call and ringtone focus. `CarPlayServiceManager` normally retains media focus through a pause. | One sink owns focus, while the media session handles buttons. Calls and Siri precede music; navigation ducks music. Keep media focus through pause, mute Geely output on focus loss, and release focus at teardown. A different source selected by the driver suppresses CarPlay music until the phone reports resumed playback. |
| Audio routing | The factory service uses framework `AUDIO_USAGE_CP_*` and `AUDIO_SOURCE_CP_*` symbols. The APK template assigns every audio usage/source the value 1. | Prefer symbols present in the installed framework, then usable vendor configuration values. Ignore non-media template values that would route everything as music. Preserve explicit stream overrides and Android fallbacks. Factory microphone sources fall back on creation or recording failure. |
| Bluetooth | `ReconnectManager` uses Android Automotive's A2DP-sink profile (11), disconnecting only the active CarPlay phone's music connection. | Recognize sink/client roles during phone selection. Guard the bonded active peer's A2DP-sink connection while its CarPlay session is active. Keep the adapter, pairing and HFP intact; release the proxy and receiver when the session ends. |
| Steering voice key | `HardKeyModel` logs `onKeyPressed : 200231` and `onKeyReleased : 200231`. The SDK also declares media key candidates 200085, 200087 and 200088. | Parse pressed/released log events. Without a saved custom mapping, observe the verified factory voice-key log to request Siri. Media controls continue through Android's media session; other system-log or broadcast mappings can be learned and saved. |
| HUD | `NavigationProxy` subscribes to guidance, but its callback only logs the received data. No complete factory HUD publishing implementation was found in these two APKs. | Use the GD secondary-display projection path. List active secondary displays, remember the user's selection, keep the background transparent, draw consistent arrow paths, and remove guidance after 30 seconds without an update. Automatic mode prefers a display named HUD or the only available secondary display. |

## Scope and fallbacks

- Detection uses G636 / FX11 / KX11 / Geely Android identity, a Geely vendor configuration, or the installed factory receiver together with its factory UI or Geely system service. This does not grant system permissions.
- A user-selected image and other custom icon labels remain supported. The previous automatic BYD label is replaced with the Geely label on a detected Geely head unit.
- Audio focus is enabled by default on detected Geely units. An explicitly saved audio-focus setting and manual stream selections remain effective. Other units retain their existing artwork and default settings.
- When no explicit navigation channel has been saved, navigation uses the installed factory CarPlay guidance usage or Android navigation usage. An explicit selection, including an older saved stream number, remains authoritative. The inspected official receiver does not establish a universal Geely stream 14 or a headrest stream number.
- The 0.2.11 merge retains upstream telephone communication mode, available platform echo cancellation and noise suppression, and microphone diagnostics. Each microphone-source attempt owns its effects; rejected attempts release them before trying the Android fallback. Call teardown restores the previous audio mode.
- Runtime reflection and restricted Bluetooth methods can be unavailable to an ordinary installed app. Rejected operations are recorded in technical diagnostics and fall back without disabling Bluetooth or changing system application settings.
- The factory receiver service and DiPlay must not own the same wired phone session simultaneously. This change does not disable, stop or replace the factory service.
- A disconnected A2DP-sink link is not forcibly reconnected at teardown; the head unit resumes its normal Bluetooth policy after the guard closes.
- Steering learning reads DiPlay's own system-log stream and broadcast receivers. It does not use GD / OneOS callbacks as a learning source. The separate legacy steering option remains opt-in; enabling it suppresses the automatic factory log binding to avoid duplicate voice requests.
- Learning car-side key events needs useful system events and permission to read them, but does not need an iPhone. Sending those controls to CarPlay does require a connected iPhone.
- Native HUD communication through CAN, a vendor service or an undisclosed protocol is not implemented by this overlay. The projection selector lists secondary displays exposed to Android; the user chooses the actual HUD target when the firmware exposes more than one.
- Accessory authentication, certificates, serial numbers and product-plan identifiers are not copied from the factory APKs or advertised by this adaptation.

## Validation limits

Source inspection and the supplied Xingyue L diagnostic report confirm the control paths described above. Compilation and existing automated checks run through the requested GitHub Actions workflow; their result is reported separately. Factory icon file permissions, custom audio-policy behavior, Bluetooth privileges, microphone recording and HUD output require a real vehicle session to establish hardware compatibility. No vehicle success is claimed from a successful build.
