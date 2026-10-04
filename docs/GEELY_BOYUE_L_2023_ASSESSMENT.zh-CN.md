> 2026-10-04 更新：本文为原 0.2.10 基线分析。已开始整合 carlito 的吉利按键、音频和 HUD 提示适配，详见 [0.2.11-geely.1 集成记录](FORK-RELEASE-0.2.11-GEELY.1.md)。完整仪表地图仍未实车确认。

# 2023 款博越 L / 银河 OS 2.5.0 适配评估

评估日期：2026-10-03。用户确认可以安装第三方 APK，需要仪表显示完整地图及方向盘切歌；手机主要使用高德地图，车机可以连接 ADB，但希望尽量减少接电脑和手工操作。

代码基线：`CoderLin99/DiPlay` 的 `main`，提交 `45563135a3d05a17315a84f57434fdb5dfa43a5f`。拉取时与上游 `shihabal3amri/DiPlay:main` 一致；应用版本为 `0.2.10` / versionCode `29`。中文 README 仍写 0.2.9，版本判断以构建文件为准。

这是源码评估和实施方案，尚未在目标车辆运行；车机 Android 版本、固件 fingerprint、仪表显示权限、实际按键路由及手机地图支持情况均待采集。银河 OS 2.5.0 不是 Android API 版本号。

## 结论

| 需求 | 当前代码具备的能力 | 对目标车的判断 |
| --- | --- | --- |
| 中控 CarPlay | 有线、无线连接及音视频接收 | 满足 Android/API、网络、USB 等条件后可试，尚未实车验证 |
| 方向盘上一曲/下一曲 | 标准 Android MediaSession、音频焦点、媒体键到 CarPlay HID 转发 | 有现成通用实现；是否直接可用取决于银河 OS 的事件路由 |
| 仪表完整地图 | CarPlay 第二屏请求、视频解码、BYD 仪表 Presentation | 没有博越 L 适配，当前代码不能作为该车已支持的依据 |
| 转向箭头/路名 | iAP2 导航数据解析及 BYD 导航输出 | 也依赖厂家接口；不能代替完整地图，也不能由箭头数据还原完整地图 |

在 `common`、`shared`、`mobile`、`automotive` 的源码/清单及现有文档中检索，未发现 Geely、ECARX、Boyue、吉利或博越适配。上游 README 明确将官方支持范围限定为比亚迪。

## 方向盘切歌：先验证现成链路，再补吉利入口

现有链路：

```text
Android 媒体按键 / MediaController
  → CarPlayMediaCallback
  → CarPlayController.sendMediaButton()
  → CarPlay media HID
  → iPhone 播放器
```

源码位置（行号对应本次基线）：

- `common/src/main/java/com/shilapi/xcertplay/CarPlayMediaKeys.kt:147`：媒体音频开始时创建会话，暂停后保留会话，支持再次播放。
- 同文件 `:155`：申请 `USAGE_MEDIA` 音频焦点；`:173` 创建并激活 MediaSession。
- 同文件 `:287`：处理媒体键，仅接受首次 ACTION_DOWN，避免长按重复触发；`:299`、`:300` 处理下一曲、上一曲。
- `shared/src/main/java/com/shilapi/xcertplay/airplay/CarPlayMediaButton.kt`：已映射标准 `KEYCODE_MEDIA_NEXT/PREVIOUS`，另外有 BYD 专用键码。
- `shared/src/main/java/com/shilapi/xcertplay/orchestration/CarPlayController.kt:430`：将指令发给当前 CarPlay 会话。

标准入口没有限定必须是 BYD。如果银河 OS 把方向盘按键交给当前 Android 媒体会话，切歌可以直接工作。但音频焦点成功不等于厂家一定会路由按键；也不能通过增加几个键码解决系统根本没有投递事件的问题。

推荐按以下顺序处理：

1. 连接 CarPlay，先从手机实际播放音乐，让 DiPlay 建立 MediaSession。
2. 验证标准媒体命令能否切歌，再按实体方向盘键对比；同时观察原车音乐是否被误触发。
3. 标准命令成功、实体键失败时，定位厂家输入/媒体中心接口。新增可选 `GeelyMediaBridge`，把已确认的下一曲、上一曲、播放暂停事件送入现有媒体分发入口，保留车端视频播放时的现有分流行为。
4. 如果车机需要媒体源注册，再接入与该固件匹配的媒体中心服务，发布来源、播放状态和现有歌曲元数据。服务绑定、权限拒绝、断线恢复需要分别处理。
5. 同一按键只使用一条有效入口，避免标准会话和厂家回调同时生效导致一次跳两首。停止 CarPlay 时释放监听及来源状态。

公开参考项目 [GInputBridge](https://github.com/Salat39/GInputBridge) 中可以观察到以下接口线索：

- `KeyInputManager.registerListener(...)`：吉利 OneOS 输入回调。
- `com.ecarx.xui.adaptapi.input.Input.requestKeysInterception(...)`：亿咖通输入接口。
- 将事件通过 `MediaController.dispatchMediaButtonEvent()` 或 `transportControls.skipToNext/Previous()` 转发给 Android 播放器。

参考快照为 `1d7d1d8bcf6e7dd9f4ac12857f388c1176bae4fa`。这是其他开发者项目的实现线索，不是博越 L / 银河 OS 2.5.0 的兼容性或权限保证。先确认目标固件是否包含对应类与服务，不直接套用 OneOS 的键码、权限和包名。本次没有引入该项目代码。

## 完整地图：手机端能力已有，缺的是吉利仪表输出通道

现有链路：

```text
iPhone 地图提供仪表第二屏
  → DiPlay 宣告第二屏及地图 initialURL
  → 接收 stream type 111
  → MediaCodec 解码到独立 Surface
  → BYD 仪表 Presentation
```

关键证据：

- `shared/src/main/java/com/shilapi/xcertplay/airplay/CarPlayClusterDisplay.kt:12`：已有 `maps:/car/instrumentcluster/map`，不是中控屏截图。
- `common/src/main/java/com/shilapi/xcertplay/ClusterMapPresentation.kt:138`：只枚举 presentation 类显示器，再交给 BYD 选择逻辑。
- `common/src/main/java/com/shilapi/xcertplay/DiLink51ClusterLayout.kt:11`、`:59`：匹配 `fission_bg_XDJAScreenProjection` 及其 shared 显示层；没有吉利配置。
- `common/src/main/java/com/shilapi/xcertplay/CarPlayHostActivity.kt:747`：找不到匹配显示器便返回 null，不向手机提供仪表显示配置。
- `common/src/main/java/com/shilapi/xcertplay/DiPlayActivity.kt:323`：地图设置入口也受匹配显示器条件限制。

所以仅打开 BYD 地图开关、授予悬浮窗权限或修改品牌字符串，不能完成博越 L 适配。

### 路线 A：存在可供应用使用的仪表 Display

先用小型探测界面比较普通应用可见的 `DisplayManager.displays` 与 `DISPLAY_CATEGORY_PRESENTATION`，记录名字、尺寸、flags、状态和显示/移除事件。ADB shell 能看到的显示器，不一定对普通应用开放。

在用户手动打开原车仪表地图前后各采一次，确认哪个显示器实际对应仪表地图区域。用短时、可自动退出的定位网格验证输出区域及权限，再接 CarPlay 视频。

实现上提取 `ClusterDisplayProvider`（显示器发现、可用性、尺寸及地图区域）和 `ClusterViewportProfile`（裁剪、安全区域、主题），保留 BYD 实现，增加经实车数据确认的 Geely 实现。复用第二路视频接收和解码，并处理 Display 消失/重建。

不能直接选“第一个非主屏”：它可能是其他屏幕、系统虚拟屏或无输出的投影层。现有 `CarPlayClusterDisplay` 的默认安全区域也是 BYD 测量值，需一起移出，不应带到吉利仪表。

验收目标是地图填充原车允许的导航区域，同时保留车速、挡位、告警等原生信息。用户要求完整地图不等于覆盖整块仪表或去掉原车信息。

### 路线 B：仪表由厂家服务或另一个系统控制

如果普通应用无法获得可用 Display/Surface，继续调查原厂导航如何向仪表提供画面：对应 APK、共享库、服务接口、Surface 的提供/消费方向、导航显示焦点、调用者权限和包名白名单。

参考项目存在 `DimInteraction`、`INaviInteraction`、`NaviAPI.switchMapPresentation`、`NaviManager.addSurface` 等名称，但仅凭这些名称不能认定可把 DiPlay 视频送入仪表。例如向导航服务传 Surface 可能是在请求导航服务画图，并不是获得仪表输出 Surface。

确认有可用输出接口后，新增 `GeelyClusterBridge`，把第二路解码结果接入该接口并跟随原车地图区域生命周期。如果输出需要 OEM 签名/授权，而车上没有可用授权路径，则“普通第三方 APK”方案无法保证实现；需要厂商支持的接入方式。单纯拥有 ADB 或安装权限不代表拥有仪表输出权限。

### 手机地图也是单独的验证门槛

上游文档实际验证的是 Apple Maps 配合特定 iOS/BYD 固件。高德、百度等应用在 CarPlay 主屏能显示地图，不代表也能提供这条仪表第二屏流。

Apple 提供第三方导航应用的仪表场景 API，说明该能力并非只允许苹果地图使用。但没有找到足以确认当前高德版本与本项目第二屏实现兼容的证据，也不能把 CarPlay 中控的 Dashboard 分屏与实体仪表混为一谈。

先用苹果地图验证车端输出和解码，再验证用户的高德/iOS 版本及 `altScreenURLs`/实际视频输出。可先在测试包的中控诊断窗口预览第二路流，把“手机是否提供高德仪表地图”和“吉利仪表能否接收画面”分开验证。该诊断预览需要开发，现有代码仍会在没有匹配 BYD 仪表时禁用第二屏。

若目标地图不提供第二屏，这一模式不能保证显示该应用的完整地图。另接车机本地导航属于另一套路线，不能承诺与手机原路线自动同步。

## 优先采用不接电脑的验证流程

ADB 不是日常使用或第一轮验证的必选项。按用户希望减少操作的要求，优先顺序调整为：

1. 安装上游正式提供的 0.2.10 预览 APK，连接手机，播放一首音乐，直接尝试方向盘上一曲/下一曲。现有标准链路若有效，不需要先做 ADB 配置。
2. 如果基本连接有问题，使用现有应用的“导出诊断”功能。当前报告已有 Android/API、车机型号、构建信息、连接及主屏协商日志，但不能当作完整的吉利显示器/按键探测报告。
3. 下一步开发优先增加应用内的“一键兼容性检测”：列出本应用可见显示器、记录自身媒体会话回调、记录权限/窗口创建结果，以及可选的第二路地图诊断预览。用户只需安装测试包、按提示操作并导出结果。本次只提出该方案，尚未实现或产出测试 APK。
4. 只有普通应用无法获得必要信息，或确认厂家接口要求额外授权时，才用一次 ADB 补充取证/设置。目标是把必要步骤集中到一次，而不是要求用户长期接电脑；是否可做到完全免 ADB，取决于目标固件。

应用内检测不能绕过系统签名权限，也不能保证看到被系统完全截获的方向盘键。即便用户可以使用 ADB，也不等于 ADB 一定能够授予 OEM 私有权限。

## 最小实车取证与判断顺序

以下为按需使用的开发排查附录，不要求用户现在执行。前提：车辆停稳，已有用户授权的 ADB 连接。以下步骤不假定博越 L 的 ADB 开启方式、端口或权限；本次没有连接或操作车辆。

先采集系统版本及显示器概况：

```powershell
adb devices
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.fingerprint
adb shell dumpsys display > display-before.txt
```

用户手动把原车地图送到仪表，再采集：

```powershell
adb shell dumpsys display > display-stock-map.txt
adb shell dumpsys window displays > window-displays-stock-map.txt
adb shell service list > services.txt
adb shell pm list libraries > shared-libraries.txt
adb shell pm list packages > packages.txt
```

连接 DiPlay 并开始播放音乐后：

```powershell
adb shell dumpsys media_session > media-session.txt
adb shell cmd media_session help
adb logcat -v threadtime 'DiPlay-MediaKeys:I' 'DiPlay-Cluster:I' '*:S'
```

若车机 `cmd media_session help` 列出了 `dispatch`，在另一终端依次测试：

```powershell
adb shell cmd media_session dispatch next
adb shell cmd media_session dispatch previous
```

观察手机是否切歌，然后测试实体键。上述两条指令会实际控制当前选中的媒体会话；先确认当前会话属于 DiPlay。`sent=true` 只代表本地指令已提交，不代替观察 iPhone 是否切歌。按 Ctrl+C 结束日志查看；若设备拒绝某个 dumpsys/命令，保留拒绝结果作为权限证据。

| 实测结果 | 下一步 |
| --- | --- |
| 标准命令和实体按键都有效 | 无需另造吉利按键桥；继续测试暂停恢复、后台、来电和重连 |
| 标准命令有效，实体按键无日志 | 调查银河 OS 输入/媒体中心路由，补 GeelyMediaBridge |
| 两者都无效 | 先排查会话是否建立、焦点、选中媒体源、CarPlay 发送链路 |
| 有 app 可用的仪表显示器且定位网格可见 | 走路线 A，接第二路地图视频 |
| 只有 shell 可见或 Presentation 权限失败 | 调查厂家显示服务及调用权限 |
| 没有输出通道，或接口只接受转向信息 | 不能宣称支持完整地图；继续获取原厂投影接口证据 |

开发顺序：基础 CarPlay → 标准切歌 → 按需增加吉利按键入口 → 仪表输出探测 → 第二屏地图 → 后台/主题/重连验证。源码测试可以验证键码转换、去重、显示器筛选和尺寸计算，但无法证明目标车的权限与实际画面。

## 构建注意点及资料

本次仅拉取和审查代码，未构建 APK、未运行车端测试、未修改生产逻辑。后续构建需按 `docs/BUILD.md` 准备 JDK/Android SDK/NDK。普通 source-only debug 构建不包含 CarPlay 配件认证身份，不能把连不上 iPhone 误判成吉利不兼容；可连接 iPhone 的实车测试包需项目支持的显式运行时认证配置。

- [目标仓库](https://github.com/CoderLin99/DiPlay)
- [上游 BYD 仪表地图说明](https://github.com/shihabal3amri/DiPlay/blob/main/docs/BYD_NAVIGATION.md)
- [上游兼容性范围](https://github.com/shihabal3amri/DiPlay/blob/main/docs/COMPATIBILITY.md)
- [Android Presentation](https://developer.android.com/reference/android/app/Presentation)
- [Android 媒体按键路由](https://developer.android.com/media/legacy/media-buttons)
- [Apple：第三方导航应用的仪表地图支持](https://developer.apple.com/videos/play/wwdc2022/10016/)
- [GInputBridge 输入及媒体桥接实现](https://github.com/Salat39/GInputBridge/blob/1d7d1d8bcf6e7dd9f4ac12857f388c1176bae4fa/app/src/main/java/com/salat/gbinder/App.kt)
