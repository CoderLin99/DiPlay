# DiPlay 0.2.10 — 2026-10-03 更新

## 本次更新

- 在兼容的桌面和媒体界面上显示歌曲、艺人、播放进度及专辑封面。
- 改善有线连接启动，并减少无线连接初始化失败的情况。
- 通话可使用车机支持的回声消除和降噪，结束后恢复原有音频状态。
- 新增主界面地图跟随仪表地图的显示开关，可单独隐藏主界面地图。
- 支持的视频播放可拖动进度，并向前或向后跳转 10 秒；仅在驻车条件满足的车机上可用。
- 改善兼容比亚迪车型的电量信息读取，并补充乌克兰语界面翻译。

## 吉利适配与方向盘按键

- HUD 投影会列出车机开放的可用副屏，可由用户选择并记住目标屏幕；诊断报告会记录副屏识别和投影状态，便于排查 HUD 无显示问题。
- 设置中可选择并裁剪 CarPlay 返回车机桌面的图标，也可随时恢复默认图标。
- 诊断页面新增故障描述和一键上传云端，只有用户主动点击后才会投送报告；单份报告上限为 1 MiB。
- 修复吉利原厂环境中导航声音未使用独立导航通道的问题；已手动选择的通道保持不变。
- 修复部分车型无线 CarPlay 与原车蓝牙音乐争抢播放通道的问题。
- 改善部分 Android 11 车机隐藏系统栏后的全屏恢复，减少画面底部被系统栏占用。
- 媒体方控随当前 CarPlay 音源接管，减少按键被原车蓝牙播放器截获。
- 保留原厂返回桌面图标、音频协调、蓝牙音乐切换、原厂语音键识别和 HUD 设置。
- 方控继续通过本应用的按键识别功能配置，支持保存、导出和上传云端。
- 更新后继续使用已有配置和配对信息。

功能效果取决于车机支持的能力和权限。本次音频通道修复适用于发出蓝牙音乐交接请求的车型；星越 L KX11 已根据用户诊断日志完成针对性适配，仍需安装后复测。吉利 G636 / FX11 仍需连接 iPhone 后实车确认。HUD 导航仅支持车机开放的 HUD 显示屏。

## What's new

- Show song details, playback position and album artwork on compatible launchers and media displays.
- Improve wired startup and reduce wireless initialization failures.
- Use available head-unit echo cancellation and noise suppression for calls, restoring the previous audio mode afterward.
- Add a switch to hide the home-screen dashboard-map mirror independently.
- Seek supported videos and skip backward or forward by ten seconds on head units that meet the parked-playback requirements.
- Improve battery information on compatible BYD vehicles and extend Ukrainian translations.

## Geely adaptation and steering controls

- HUD projection lists the secondary displays exposed by the head unit, lets the user choose and remember the target, and records display detection and projection status in diagnostic reports.
- Choose and crop the CarPlay return-to-home icon in Settings, or restore the default.
- Add an issue-description field and one-tap diagnostic upload; reports are sent only after the user taps upload and are limited to 1 MiB each.
- Route guidance through the separate factory navigation channel on Geely head units while preserving a channel selected by the user.
- Fix factory Bluetooth music competing with wireless CarPlay for audio on affected head units.
- Improve full-screen recovery after hiding the system bars on affected Android 11 head units.
- Keep media steering controls with the active CarPlay source instead of the factory Bluetooth player.
- Retain factory return-to-home artwork, coordinated audio, Bluetooth music handoff, factory voice-button recognition and HUD settings.
- Continue identifying steering buttons within the app, with saved configurations, export and cloud upload.
- Retain existing settings and pairing records when updating.

Features depend on the head unit's capabilities and permissions. The audio-channel fix applies when a head unit receives the Bluetooth music handoff request. The Xingyue L KX11 adaptation is based on a user diagnostic report and still needs an installed retest; Geely G636 / FX11 behavior still needs vehicle verification with an iPhone. HUD projection requires a secondary display exposed by the head unit.
