# DiPlay 0.2.9 — 2026-10-03 更新

## 吉利车机适配

- CarPlay 的返回桌面按钮优先使用车机自带的 Geely 图标；无法读取时使用桌面图标。
- 优化音乐、导航、Siri 和通话的声音切换，减少同时播放和抢占音源的情况。
- 改善当前 CarPlay 手机与车机蓝牙音乐的切换，保留其他设备的连接和配对。
- 支持识别原厂语音按键的按下与松开信息，媒体按键仍可通过按键识别进行配置。
- HUD 导航仅显示在车机提供的 HUD 屏幕上，采用透明背景，并自动清除过期提示。

吉利 G636 / FX11 的实际效果取决于车机开放的功能和权限，仍需连接 iPhone 后实车确认。

## 方向盘按键

- 新增“设置 → 按键识别”，可将方向盘按钮设置为播放/暂停、下一曲、上一曲或 Siri。
- 选择功能后按下对应按钮，识别结果会自动填入，保存后立即生效。
- 按车型和车机型号保存配置并上传云端；断网时保留配置，恢复联网后自动继续上传。
- 支持导出已保存的配置，也可恢复原有按键功能；恢复后仍可导出原配置。

首次识别需要车机允许读取按键信息。识别效果取决于车机提供的信息，实际车型兼容性仍需实车确认。

## Geely head units

- Use the head unit’s Geely artwork for CarPlay’s return-to-home button when available, with a house icon as the fallback.
- Improve switching between music, navigation, Siri and calls, reducing competing audio sources.
- Improve Bluetooth music handoff for the active CarPlay phone while retaining other devices and pairings.
- Recognize factory voice-button press and release information. Other media controls can be configured through button identification.
- Limit HUD navigation to an available HUD display, use a transparent background and clear expired guidance.

Actual behavior on Geely G636 / FX11 depends on the head unit’s capabilities and permissions and still needs vehicle verification with an iPhone.

## Steering wheel controls

- Add **Settings → Identify steering buttons**, with assignments for play/pause, next track, previous track and Siri.
- Choose an action and press its button. Identification fills the assignment automatically; Save applies it immediately.
- Save configurations by vehicle and head unit model and upload them to the cloud. Saved configurations are retained offline and uploads resume when internet access returns.
- Export saved configurations or restore the original controls. Restoring retains the saved configuration for export.

Identification requires the head unit to allow button access and provide useful button information. Compatibility with individual vehicles still needs physical verification.
