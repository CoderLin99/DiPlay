# DiPlay 0.2.12 — 2026-10-05 更新

## 本次更新

- 新增自动无线连接：优先使用车机当前已开启的热点，无法使用时自动尝试其他兼容方式。
- Android 9 车机可在系统允许时读取当前热点名称和密码，避免保存的旧密码导致反复连接失败。
- 新增“同一 Wi-Fi”连接方式，车机与 iPhone 可通过同一路由器或便携热点连接。
- 改善无线连接准备和重试，减少热点已经开启却一直停在准备界面的情况。
- 改善 Wi-Fi Direct 连接稳定性，并保留首选信道设置。
- 改善 iPhone USB 接入和授权提示识别，避免误操作无关窗口。
- 改善分屏、短屏和桌面卡片中的画面比例、触控位置与系统栏显示。
- 新增跟随系统、光线、固定日间和固定夜间的 CarPlay 外观模式。
- 自定义分辨率现支持 30%–160%，并会在车机不支持时自动回到合适的显示比例。
- 增加方向盘控制仪表地图缩放和主屏焦点移动的可选功能。
- 改善仪表转向卡片、歌曲显示和专辑封面的更新与恢复。
- 诊断报告可在下载目录不可用时继续保存，并提供查看和分享入口。
- 优化画面播放兼容性与延迟控制，并根据车机能力选择合适的播放方式。
- 新增导航输出设备选择、设备编号输入和试听，方便确认所需扬声器。
- 改进 USB 通话结束后的音乐播放恢复。
- 改善方向盘按键读取状态与授权失败提示，识别无响应时可收集日志反馈。

## 吉利车机适配

- 改善更多老款吉利车机的热点识别，优先使用更兼容的网络地址。
- 方向盘按键识别可学习更多 OneOS 车机上报的按键，并正确区分按下与松开。
- 保留吉利车机的音乐、导航、通话和语音助手音频切换，以及蓝牙音乐交接。
- 恢复设置中的方向盘按键识别、兼容模式和 HUD 开关，并提供投影屏幕选择与内容大小调整。
- 修复 HUD 导航提示显示，恢复 G636、FX11、KX11 的原车图标、自定义返回桌面图标和音乐播放后的音频切换。
- 恢复日志收集中的问题说明和主动上传云端功能。
- 恢复“关于”页面的版本入口、来源信息和版本点击解锁的诊断入口。

同一 Wi-Fi 连接需要路由器允许设备互相访问。HUD、原车音频和方向盘功能取决于车机开放的能力；升级后无需卸载旧版本。

## What’s new

- Add automatic wireless connection that first uses an active head-unit hotspot, then tries other compatible connection methods when needed.
- On Android 9 head units, use the current hotspot name and password when the system makes them available, avoiding retries with outdated saved details.
- Add Same Wi-Fi connection through a shared router or portable hotspot.
- Improve wireless preparation and bounded recovery when a hotspot is already on but CarPlay remains on the preparation screen.
- Improve Wi-Fi Direct reliability while retaining the preferred-channel setting.
- Improve iPhone USB attachment and permission-prompt matching without accepting unrelated dialogs.
- Improve video proportions, touch alignment and system bars in split screen, compact layouts and launcher cards.
- Add system, ambient-light, fixed-day and fixed-night CarPlay appearance modes.
- Allow custom resolution from 30% to 160%, with a compatible fallback when the head unit cannot use the requested size.
- Add optional steering-wheel controls for dashboard-map zoom and main-screen focus movement.
- Improve dashboard turn cards, song display and album-art continuity.
- Keep diagnostic reports available when the normal Downloads location cannot be used, with View and Share actions.
- Improve video playback compatibility and latency control, adapting playback to the head unit’s capabilities.
- Add navigation output selection, a device-number field and a test sound to help identify the desired speaker.
- Improve music playback recovery after USB phone calls.
- Improve steering-button access status and authorization messages, with a log-report option when identification receives no response.

## Geely adaptation

- Improve hotspot detection on more legacy Geely head units and prefer the more compatible network address.
- Let steering-button identification learn more OneOS key codes while correctly matching press and release events.
- Retain Geely audio switching for music, guidance, calls and the voice assistant, including Bluetooth music handoff.
- Restore steering-button identification, compatibility mode and HUD settings, including display selection and content size.
- Fix HUD navigation guidance and restore factory icons, custom home icons and audio switching when music playback resumes on G636, FX11 and KX11.
- Restore problem descriptions and user-triggered diagnostic uploads to the cloud.
- Restore the About page’s version control, source information and diagnostics unlocked by tapping the version.

Same Wi-Fi requires the router to allow communication between connected devices. HUD, factory audio and steering controls depend on capabilities exposed by the head unit. The update can be installed over the previous version.
