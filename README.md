# APK去广告工具

## 功能特点

- ✅ **无需Root权限** - 使用PackageManager API提取APK
- ✅ **广告自动检测** - 内置15+常见广告SDK特征库
- ✅ **一键去广告** - 自动修改并重新打包
- ✅ **免费开源** - 完全免费使用

## 支持的广告SDK

| 广告SDK | 公司 |
|---------|------|
| 穿山甲广告 | 字节跳动 |
| 广点通广告 | 腾讯 |
| 百度广告 | 百度 |
| 快手广告 | 快手 |
| Sigmob广告 | Sigmob |
| Unity Ads | Unity |
| AdMob广告 | Google |
| Mintegral | Mintegral |
| IronSource | IronSource |
| Vungle | Vungle |
| AppLovin | AppLovin |
| Chartboost | Chartboost |
| InMobi | InMobi |
| StartApp | StartApp |
| Yandex Ads | Yandex |

## 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34

## 构建步骤

1. **打开项目**
   ```
   用Android Studio打开 AdRemover 文件夹
   ```

2. **同步Gradle**
   ```
   等待Gradle同步完成（首次可能需要下载依赖）
   ```

3. **构建APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```

4. **获取APK**
   ```
   构建完成后，APK位于：
   app/build/outputs/apk/debug/app-debug.apk
   ```

## 使用说明

1. **安装应用**
   - 将构建好的APK安装到手机
   - 首次运行需要授予"查询所有应用"权限

2. **查看应用列表**
   - 打开应用后会显示所有已安装的第三方应用
   - 可以使用搜索功能查找特定应用

3. **分析应用**
   - 点击应用右侧的"分析"按钮
   - 查看应用是否包含已知广告SDK

4. **去除广告**
   - 点击"去广告"按钮开始处理
   - 处理完成后，APK会保存到 `下载/AdRemover/` 目录

5. **安装修改后的APK**
   - 找到保存的APK文件
   - 卸载原版应用
   - 安装修改后的APK

## 注意事项

⚠️ **签名变化**
- 修改后的APK签名会改变
- 无法直接覆盖安装，需要先卸载原版
- 应用数据会丢失（游戏存档、聊天记录等）

⚠️ **兼容性问题**
- 部分应用有签名校验，修改后可能无法运行
- 加固保护的应用可能无法处理
- 建议先分析再决定是否去广告

⚠️ **法律风险**
- 本工具仅供学习研究使用
- 请勿用于商业用途
- 去广告可能违反应用服务条款

## 技术原理

```
APK文件
    ↓
PackageManager API (提取APK)
    ↓
ZIP解压 (获取DEX文件)
    ↓
广告特征匹配 (扫描DEX中的广告SDK包名)
    ↓
修改DEX文件 (替换广告包名路径)
    ↓
重新打包 (ZIP压缩)
    ↓
APK签名 (使用自签名证书)
    ↓
输出去广告APK
```

## 项目结构

```
AdRemover/
├── app/
│   ├── build.gradle.kts          # 应用构建配置
│   └── src/main/
│       ├── AndroidManifest.xml   # 权限声明
│       ├── java/com/example/adremover/
│       │   ├── MainActivity.kt   # 主Activity
│       │   ├── MainViewModel.kt  # ViewModel
│       │   ├── core/
│       │   │   ├── AppExtractor.kt    # APK提取器
│       │   │   ├── AdAnalyzer.kt      # 广告检测器
│       │   │   ├── SmaliPatcher.kt    # DEX修改器
│       │   │   ├── ApkSignerUtil.kt   # APK签名器
│       │   │   └── AdRemoverEngine.kt # 核心引擎
│       │   ├── model/
│       │   │   └── AppInfo.kt   # 数据模型
│       │   └── ui/
│       │       └── MainScreen.kt # UI界面
│       └── res/
│           ├── values/          # 资源文件
│           └── xml/             # FileProvider配置
├── build.gradle.kts             # 项目构建配置
├── settings.gradle.kts          # 项目设置
└── README.md                    # 说明文档
```

## 常见问题

### Q: 为什么有些应用去广告后无法打开？
A: 可能是应用有签名校验或完整性检查，修改后触发了保护机制。

### Q: 能否保留应用数据覆盖安装？
A: 不能。签名不同时，Android系统强制要求卸载原版才能安装。

### Q: 处理后的APK在哪里？
A: 保存在手机的 `下载/AdRemover/` 目录。

### Q: 支持哪些Android版本？
A: 支持Android 8.0 (API 26) 及以上版本。

## 更新日志

### v1.0.0 (2026-06-28)
- 初始版本
- 支持15+广告SDK检测
- 实现APK提取、分析、修改、签名全流程

## 许可证

MIT License

## 免责声明

本工具仅供学习和研究使用。使用本工具产生的任何后果由用户自行承担。请遵守当地法律法规和应用服务条款。
