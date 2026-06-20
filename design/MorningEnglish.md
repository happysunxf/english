# MorningEnglish 产品设计方案

> **目标**:6 岁+ 儿童,每天早上 6:30 自动播放一段英语学习视频,初级难度,家长零操作。
> **代号**:`MorningEnglish`
> **平台**:Android 原生 (Kotlin + Jetpack Compose)
> **版本**:v0.1 草案(2026-06-21)

---

## 一、产品定位(为什么这么做)

| 维度 | 决策 | 理由 |
|------|------|------|
| **用户** | 单个学龄前/小学儿童 | 不用做"多用户"复杂度 |
| **场景** | 早起洗漱/早餐时**被动听** | 关键洞察:不是"主动学",是**"背景音渗透"** —— 类似磨耳朵 |
| **交互** | **0 交互**(自动播) | 6 岁孩子玩 app 会分心,6:30 响了直接播视频 |
| **时长** | 每天 1 段,**3-5 分钟** | 超过 5 分钟注意力流失 |
| **难度** | 初级(Start with 0基础) | 后续根据接受度手动调档,不自动推 |
| **平台** | **Android 原生** | 直接打包 APK 装孩子设备 |
| **付费** | **完全免费** | 不接广告(广告对孩子是污染),不开会员 |
| **更新** | 内容本地存,**首次联网拉取** | 离线播放,断网也不影响早上 6:30 |

---

## 二、素材来源(最难的环节)

这是**整个项目的瓶颈**——技术不难,内容难找。

### 方案 A:用 YouTube 现有动画(最省事,推荐起步)

| 候选 | 内容 | 风格 | 时长 | 风险 |
|------|------|------|------|------|
| **Dora the Explorer** | 西班牙女孩冒险,每集学英语单词 | 动画+互动 | 22min | **太长**,要剪 |
| **Peppa Pig(英文版)** | 家庭日常,英式口音 | 动画 | 5min/集 | ✅ **完美匹配**——单集时长合适 |
| **Bluey** | 澳洲家庭狗动画 | 动画 | 7min/集 | ✅ 时长略长但优秀 |
| **Cocomelon** | 美国幼儿启蒙 | 动画+儿歌 | 3-4min | ✅ 极合适,但内容偏儿歌 |
| **Numberblocks / Alphablocks** | BBC 数学/字母启蒙 | 教育动画 | 5min | ✅ **极合适** |
| **Big Muzzy(英音)** | BBC 经典英语启蒙 | 动画 | 10min | 经典,但偏旧 |

**操作**:用 `yt-dlp`(命令行工具)下载这些动画的英文版 mp4,存在 app 内置 assets。

### 方案 B:自己做教学视频(最定制,工作量最大)

- 你给孩子录:**每天 1 句"Hello, how are you?"+ 动作示范**
- 优势:**亲子互动**,孩子会特别爱看
- 劣势:每天录很累,坚持 30 天后大概率放弃
- **建议**:第一周录 5 段自己录的,之后混入 Peppa Pig 等现成内容,**让孩子感受"既有爸妈的声音又有动画"**

### 方案 C:现成英语启蒙 App 的内容**扒下来**(灰色,慎用)

- 喜马拉雅少儿、宝宝巴士、叽里呱啦等
- **版权风险** + **道德风险**——不建议,容易被告
- ❌ **跳过**

### ✅ 推荐:**A+B 混合**

| 阶段 | 内容 | 占比 |
|------|------|------|
| 第 1-7 天 | 你录的简单英语短句 | 100% |
| 第 8-30 天 | 你录 + Peppa Pig 混播 | 50/50 |
| 第 30+ 天 | 你录 + Peppa + Numberblocks | 30/30/40 |

**素材获取命令**(等 app 做好后用):
```bash
# 安装 yt-dlp(单条命令,不用 sudo)
pip install --user yt-dlp

# 下载 Peppa Pig S01E01(英音)
yt-dlp -o "peppa_s01e01.mp4" "https://www.youtube.com/watch?v=0PpLfX2Irdk"

# 仅下载音频(节省空间,孩子可能不需要画面)
yt-dlp -x --audio-format mp3 "URL"
```

---

## 三、App 架构(技术方案)

### 3.1 技术栈选择

| 维度 | 推荐 | 理由 |
|------|------|------|
| **开发语言** | **Kotlin** | Android 官方语言,语法现代 |
| **最低 Android 版本** | **API 26 (Android 8.0)** | 覆盖 95%+ 设备,WorkManager 在 26+ 才稳定 |
| **视频播放** | **ExoPlayer(Media3)** | Google 官方,支持 mp4/m3u8/加密流 |
| **定时调度** | **WorkManager + AlarmManager 双保险** | 见 §3.4 详解 |
| **数据存储** | **Room(SQLite 封装)** | 存播放历史、素材索引 |
| **UI 框架** | **Jetpack Compose** | 现代,代码少 |
| **架构模式** | **MVVM + Repository** | 标准 Google 推荐 |
| **依赖管理** | **Gradle(Kotlin DSL)** | 标准 |

### 3.2 App 模块划分

```
app/
├── data/                  # 数据层
│   ├── asset/             # 视频素材管理(本地 filesDir)
│   ├── db/                # Room: 播放记录 / 素材索引
│   └── repo/              # Repository: 数据来源抽象
├── domain/                # 业务逻辑
│   ├── player/            # 播放控制
│   └── scheduler/         # 定时调度
├── ui/                    # 界面
│   ├── home/              # 主屏(今日播放列表 + 历史)
│   ├── settings/          # 设置(时间/难度/开关)
│   └── permission/        # 权限请求页(关键!)
├── service/               # 后台服务
│   ├── DailyPlayerService # 前台服务,播放视频
│   └── BootReceiver       # 开机自启广播
└── util/                  # 工具
```

### 3.3 核心数据结构(Room)

```kotlin
@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey val id: String,           // UUID
    val title: String,                    // "Peppa Pig S01E01"
    val filePath: String,                 // 内部存储路径
    val durationSec: Int,
    val difficulty: Int,                  // 1=初级 2=中级 3=高级
    val source: String,                   // "local" / "youtube"
    val tags: String,                     // "peppa,family,cartoon"
    val createTime: Long
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val videoId: String,
    val playDate: String,                 // "2026-06-21"
    val playTime: Long,                   // 时间戳
    val completed: Boolean                // 是否播完
)
```

### 3.4 最关键的部分:6:30 自动播放(双保险设计)

**问题**:Android 后台限制很严,只靠 `WorkManager` 或 `AlarmManager` 任何一个都不够稳。

**双保险架构**:

```
┌─────────────────────────────────────────────────────────┐
│  触发层(谁负责在 6:30 唤醒)                            │
│  ┌──────────────────┐    ┌──────────────────┐           │
│  │ AlarmManager     │    │ WorkManager      │           │
│  │ (精准闹钟)       │    │ (周期任务)       │           │
│  │ setExactAndAllow │    │ PeriodicWork     │           │
│  │ WhileIdle        │    │ 24h interval     │           │
│  └────────┬─────────┘    └────────┬─────────┘           │
│           │                       │                     │
│           └───────────┬───────────┘                     │
│                       ▼                                 │
│              ┌────────────────────┐                     │
│              │ BootReceiver       │ ← 开机后重注册闹钟   │
│              │ (开机自启)         │                     │
│              └────────────────────┘                     │
└─────────────────────┬───────────────────────────────────┘
                      ▼
┌─────────────────────────────────────────────────────────┐
│  执行层                                                 │
│  ┌────────────────────┐                                 │
│  │ DailyPlayerService │ ← 前台服务(FOREGROUND_SERVICE) │
│  │ - 锁屏播放         │   显示"今日英语"通知,防止被杀  │
│  │ - WakeLock         │                                 │
│  │ - 全屏 Intent      │                                 │
│  └────────────────────┘                                 │
└─────────────────────────────────────────────────────────┘
```

**核心代码骨架**:

```kotlin
// === AlarmReceiver.kt ===
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val nextVideo = VideoRepository.getNextVideo()  // 选今日要播的
        val serviceIntent = Intent(ctx, DailyPlayerService::class.java).apply {
            putExtra("videoPath", nextVideo.filePath)
            putExtra("videoTitle", nextVideo.title)
        }
        ContextCompat.startForegroundService(ctx, serviceIntent)
        
        // 注册明天的闹钟
        AlarmScheduler.scheduleNextDay(ctx)
    }
}

// === AlarmScheduler.kt ===
object AlarmScheduler {
    fun scheduleNextDay(ctx: Context) {
        val alarmMgr = ctx.getSystemService(AlarmManager::class.java)
        val intent = Intent(ctx, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            ctx, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 明天 6:30
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 30)
            set(Calendar.SECOND, 0)
        }
        
        // setExactAndAllowWhileIdle 能绕过 Doze,精准触发
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmMgr.canScheduleExactAlarms()) {
                alarmMgr.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                // 用户没授权 SCHEDULE_EXACT_ALARM,降级用 inexact
                alarmMgr.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}

// === DailyPlayerService.kt ===
class DailyPlayerService : Service() {
    private lateinit var player: ExoPlayer
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val videoPath = intent?.getStringExtra("videoPath") ?: return START_NOT_STICKY
        
        // 1. 启动前台服务(必填,否则后台被杀)
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // 2. 获取 WakeLock(防止 CPU 休眠)
        wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MorningEnglish:wakelock")
        wakeLock.acquire(10 * 60 * 1000L)  // 10 分钟上限
        
        // 3. 唤醒屏幕 + 全屏播放
        val playIntent = Intent(this, PlayerActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("videoPath", videoPath)
        }
        startActivity(playIntent)
        
        return START_NOT_STICKY
    }
}
```

### 3.5 关键技术陷阱(提前知道避免踩坑)

| 坑 | 后果 | 解决 |
|---|------|------|
| **后台被杀** | 6:30 闹钟没响 | 用**前台服务** + 加白名单(引导用户把 app 加到"电池优化白名单") |
| **Doze 模式** | 屏幕灭后闹钟不准 | `setExactAndAllowWhileIdle` + 引导用户关"省电模式" |
| **开机后闹钟失效** | 重启后不再播 | `BOOT_COMPLETED` 广播重新注册 |
| **SCHEDULE_EXACT_ALARM 权限** | Android 12+ 默认没授权 | 引导用户去系统设置手动开,**首次启动就弹窗提示** |
| **息屏播放** | 默认锁屏后 ExoPlayer 暂停 | 加 `setWakeMode(C.WAKE_MODE_NETWORK)` + WakeLock |
| **第三方 ROM 自启管理** | 小米/华为/OPPO 默认禁自启 | 检测品牌后弹**详细引导教程**,逐个引导关掉 |
| **音视频不同步** | 部分 mp4 转封装有问题 | 用 `ffmpeg -c copy` 重新转封装,或下载时用 yt-dlp 选最佳格式 |
| **存储权限** | Android 13+ 媒体权限细化 | 只用 `filesDir`(应用私有),**不要**读写公共存储 |

### 3.6 权限清单(`AndroidManifest.xml`)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" /> <!-- Android 13+ -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" /> <!-- 锁屏覆盖,可选 -->

<receiver
    android:name=".service.AlarmReceiver"
    android:enabled="true"
    android:exported="false" />

<receiver
    android:name=".service.BootReceiver"
    android:enabled="true"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />
    </intent-filter>
</receiver>

<service
    android:name=".service.DailyPlayerService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false" />
```

---

## 四、UI 设计(3 个页面就够)

### 4.1 主屏 `HomeScreen`(父母偶尔看一眼)

```
┌──────────────────────────────────┐
│  🌅 MorningEnglish                │
│                                   │
│  ┌─────────────────────────────┐ │
│  │  今日播放                    │ │
│  │  Peppa Pig S01E05           │ │
│  │  时长: 5:12  | 难度: ⭐       │ │
│  │                              │ │
│  │  [ ▶ 立即播放 ]              │ │
│  │  [ ⏭ 换一个 ]                │ │
│  └─────────────────────────────┘ │
│                                   │
│  📅 本周播放记录                  │
│  周一 ✅ Peppa Pig                │
│  周二 ✅ 你录的:How are you?      │
│  周三 ⏳ 还没播                   │
│  ...                              │
│                                   │
│  ⚙ 设置                          │
└──────────────────────────────────┘
```

### 4.2 设置 `SettingsScreen`

| 项 | 类型 | 默认 |
|---|------|------|
| 每日播放时间 | TimePicker | 06:30 |
| 启用定时播放 | Switch | ON |
| 难度筛选 | SegmentedControl | 初级 |
| 内容来源 | CheckBoxList | 勾 Peppa/你录的 |
| 锁屏覆盖播放 | Switch | ON |
| 周末是否播放 | Switch | ON |

### 4.3 播放页 `PlayerActivity`(锁屏直接拉起)

- 全屏黑底 + 居中视频
- 顶部小字显示视频标题(透明)
- 底部 5 秒后自动隐藏进度条
- 视频结束 → 写入播放历史 → 关闭屏幕(可选)

---

## 五、实施步骤(从 0 到 1,2 周跑通 MVP)

### Phase 1:环境搭建 + 视频播放(Day 1-3)

```bash
# 1. 安装 Android Studio(略)
# 2. 创建项目,MinSDK=26, TargetSDK=34, Kotlin + Compose

# 3. 加依赖(build.gradle.kts:app)
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
}

# 4. 把第一段素材放进 assets/raw/
mkdir -p app/src/main/assets/videos/
# 复制一段 Peppa Pig mp4 进去(测试用)
cp ~/Downloads/peppa_test.mp4 app/src/main/assets/videos/

# 5. 写最简播放器,能播就 OK
# 6. 在 Pixel/小米真机测试,验证能播 mp4
```

### Phase 2:定时调度(Day 4-7)

- 写 `AlarmScheduler` + `AlarmReceiver`
- 写 `DailyPlayerService`(前台服务)
- **真机测试 5 个品牌**:Pixel、小米、华为、OPPO、三星
- **测试场景**:
  - [ ] App 被杀掉,闹钟响不响?
  - [ ] 屏幕灭着,闹钟响不响?
  - [ ] 锁屏状态,能拉起播放吗?
  - [ ] 重启手机后,闹钟还在吗?
  - [ ] 关 WiFi / 飞行模式,能播吗?(应该能,本地资源)

### Phase 3:素材下载工具 + 内容管理(Day 8-10)

- 写 `download_assets.py`(本机运行):
  ```python
  #!/usr/bin/env python3
  """下载每日英语素材"""
  import subprocess
  from pathlib import Path
  
  ASSETS_DIR = Path(__file__).parent / "app/src/main/assets/videos"
  ASSETS_DIR.mkdir(parents=True, exist_ok=True)
  
  VIDEOS = [
      ("Peppa_S01E01", "https://www.youtube.com/watch?v=..."),
      ("Peppa_S01E02", "https://www.youtube.com/watch?v=..."),
      ("Numberblocks_S01E01", "https://www.youtube.com/watch?v=..."),
      # ...
  ]
  
  for name, url in VIDEOS:
      out = ASSETS_DIR / f"{name}.mp4"
      if out.exists():
          print(f"skip {name}")
          continue
      subprocess.run(["yt-dlp", "-o", str(out), url])
  ```
- 跑脚本下 30 段素材
- Room 数据库录入索引

### Phase 4:UI 完善 + 设置(Day 11-13)

- HomeScreen / SettingsScreen / PlayerActivity
- 难度筛选逻辑
- 播放历史展示

### Phase 5:真机长测(Day 14-21)

- 把 app 装到你儿子的设备上
- **真实场景跑 7 天**:每天 6:30 起来检查有没有响
- 收集 bug,迭代修复
- 这阶段主要是打磨**第三方 ROM 自启引导**的 UX

---

## 六、风险与应对

| 风险 | 概率 | 影响 | 应对 |
|------|------|------|------|
| **第三方 ROM 自启拦截** | 高 | 闹钟不响 | 写一份"开启教程"弹窗,逐个品牌引导 |
| **孩子误触退出** | 中 | 视频没播完 | 前台服务持续 5 分钟,除非彻底杀进程 |
| **存储占满** | 低 | app 无法下载新素材 | 单集 50-100MB,30 集约 3GB,默认 720p 即可 |
| **电池消耗** | 低 | 父母嫌耗电 | 每天只播放 5 分钟,前台服务时间窗短,可忽略 |
| **网盘同步删除本地文件** | 极低 | 视频消失 | 不放公共存储,只放 `filesDir`(app 私有) |
| **版权问题** | 低(个人使用) | 自用不传播 | 严守"只给自己孩子用" |
| **孩子对内容厌倦** | 中 | 失去学习兴趣 | 每 2 周换一批内容,加你录的混合 |
| **WakeLock 漏电** | 低 | 锁屏后持续耗电 | 加 10 分钟超时,超时自动释放 |

---

## 七、变现思路(可选,你说不要副业就跳过)

| 路线 | 难度 | 适合人群 |
|------|------|---------|
| **A. 开源免费,接广告** | 低 | ❌ 不推荐(对儿童不道德) |
| **B. 付费下载(一次性 9.9)** | 低 | 个人开发者 |
| **C. 家长付费订阅**(每季度内容包) | 中 | 自媒体/教育方向 |
| **D. 卖给教培机构(B端)** | 高 | 需要商务能力 |
| **E. 完全自用,不卖** | 0 | ✅ **推荐起步走这个** |

---

## 八、立即行动项

1. **选个代号 + 在 `english/design` 仓建项目**:本文件已在 `happysunxf/english/design/MorningEnglish.md` ✅
2. **下载一段 Peppa Pig mp4 放到手机里**,手动播放 1 次,确认你儿子接受英音 + 动画风格(这是**最重要**的市场验证——比任何技术方案都优先)
3. **如果决定做**,告诉我具体下一步:

| 你想做 | 我能帮你 |
|--------|---------|
| **写完整 Kotlin 代码**(项目脚手架) | ✅ 可以,但代码量大,可能分多次 |
| **写素材下载脚本 + 素材清单** | ✅ 可以,列 30 段精选 + yt-dlp 脚本 |
| **做产品需求文档(PRD)** | ✅ 可以,标准模板 |
| **只先验证"市场"(孩子爱不爱看)** | 只需要你手动播 1 集,我帮你判断下一步 |

---

## 九、建议(以你副业工程师身份)

**先做"市场验证",再做技术**:
- 第 1 步(今天):用手机手动给儿子播 Peppa Pig 英音 S01E01,看 5 分钟内他什么反应
- 第 2 步(明天):如果接受,记录下哪些片段他看得最开心
- 第 3 步(下周):再考虑要不要花 2 周做 app

**直接上 app 的风险**:花 2 周做完,孩子第二天就不爱看 Peppa Pig 了——这种情况在副业项目里见过太多次。**先用最低成本验证需求,再投入技术**。

---

## 附录 A:素材 30 段推荐清单

待 Phase 3 时补充,候选包括:
- Peppa Pig S01-S04 全集(英音)
- Numberblocks S01-S03 全集
- Alphablocks S01-S04 全集
- Bluey S01-S03 部分集(精选)
- Cocomelon 字母/数字系列
- 父母自录 5-10 段(Hello / Thank you / I love you 等基础句)

---

## 附录 B:第三方 ROM 自启引导清单

| 品牌 | 关键路径 |
|------|---------|
| 小米(MIUI) | 设置 → 电池 → 后台耗电管理 → MorningEnglish → 无限制 |
| 华为(EMUI/HarmonyOS) | 设置 → 电池 → 启动管理 → MorningEnglish → 手动管理(全开) |
| OPPO(ColorOS) | 设置 → 电池 → 更多电池设置 → 关闭"睡眠待机优化" |
| vivo(FuntouchOS) | 设置 → 电池 → 后台高耗电 → MorningEnglish → 允许 |
| 三星(One UI) | 设置 → 电池 → 后台使用限制 → 从不 |
| 一加(HydrogenOS) | 设置 → 电池 → 电池优化 → MorningEnglish → 不优化 |

---

*文档维护:hermes,2026-06-21*