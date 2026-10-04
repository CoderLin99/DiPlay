> CoderLin99 integration: this document describes the imported carlito changes. Its cloud-upload behavior has been replaced with local export and GitHub issue submission; see [fork release notes](FORK-RELEASE-0.2.11-GEELY.1.md).

# DiPlay 0.2.11 — 2026-10-04 更新

## 本次更新

- 修复 KX11 无线连接画面正常但声音仍从手机播放的问题；有线连接继续使用已验证的音频方式。
- 修复 Android 9 KX11 已开启车机热点仍停在准备中的问题。
- Android 9 KX11 不再默认接入原车方向盘按键，避免左右切歌和暂停冲突；需要时仍可在设置中开启兼容模式。
- 修复更多吉利老款车机无线连接一直停在启动中的问题。
- 修复 KX11 车机通话、语音助手和导航可能无声的问题。
- 导航播报时音乐衔接更平顺，并适配银河 E5 的原车音频输出。
- G636、FX11 和 KX11 默认接入原车方向盘按键；按键识别无需先连接调试服务。
- HUD 投影会列出车机当前开放的副屏，可选择自动匹配或手动指定目标屏幕，并记住选择。
- HUD 诊断会记录悬浮窗权限、所选屏幕、当前连接屏幕和可用副屏，便于定位无显示问题。
- 无线连接可为下一次连接选择 Wi-Fi Direct 信道；“自动”仍是默认选项。
- 仪表地图可使用可调位置和大小的转向提示卡。
- 可选择两指、三指或四指下滑打开 DiPlay 设置。
- 增加高级车辆数据设置和受支持车机的自动热点启动选项。
- 改善 Android 9 音频兼容性、连接恢复、媒体更新和诊断信息。

## 吉利车机适配

- 沿用 GD 已验证的 Android 副屏投影方式，不再要求副屏名称必须包含“HUD”。
- 保留自定义 CarPlay 返回桌面图标、导航独立通道、音频焦点和蓝牙音乐交接。
- 媒体方控、原厂语音键和方向盘按键识别可直接使用吉利车机提供的按键服务。
- 用户可填写故障并主动将脱敏诊断报告上传云端；单份报告上限为 1 MiB。

本次合并沿用上游版本号 **0.2.11 / code 30**。HUD 投影仍要求车机把对应副屏开放给 Android；多副屏车机请在设置中选择实际 HUD 屏幕。

## What’s new

- Restore wireless CarPlay sound on KX11 while keeping the proven wired audio path.
- Recognize the hotspot interface used by Android 9 KX11 head units so wireless CarPlay can leave the preparation screen.
- Leave direct steering controls off by default on Android 9 KX11 to avoid playback-control conflicts; the compatibility option remains available in settings.
- Fix wireless CarPlay remaining on the starting screen on more legacy Geely head units.
- Restore calls, voice assistant audio and guidance on KX11 head units.
- Keep music smoother during guidance and support the Galaxy E5 factory audio output.
- Enable direct steering controls and button identification on G636, FX11 and KX11 without requiring log access first.
- HUD projection lists active secondary displays, supports automatic or manual selection, and remembers the target.
- HUD diagnostics record overlay permission, the selected and attached screens, and all available secondary displays.
- Choose a preferred Wi-Fi Direct channel for the next connection while keeping Auto as the default.
- Use a movable, resizable dashboard turn card and a configurable two-, three- or four-finger settings gesture.
- Add advanced vehicle-data settings, optional hotspot startup on supported head units, Android 9 audio compatibility improvements, connection recovery and richer diagnostics.

## Geely adaptation

- Use the GD secondary-display projection path without requiring the display name to contain “HUD”.
- Retain the custom CarPlay home icon, separate guidance channel, audio focus and Bluetooth music handoff.
- Use the Geely head unit's button service directly for media controls, the factory voice key and button identification.
- Let the user describe a problem and explicitly upload a redacted diagnostic report to the cloud, limited to 1 MiB per report.

This merge keeps the upstream version **0.2.11 / code 30**. HUD projection still requires the head unit to expose the target as an Android secondary display.
