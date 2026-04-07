# 妙笔 AI 写小说 Android App — 部署文档 & 运维指南

> 文档版本：v2.1.0 | 更新日期：2026-04-07
> 适用项目版本：妙笔 v2.1.0

---

## 目录

1. [本地开发环境搭建](#1-本地开发环境搭建)
2. [项目构建指南](#2-项目构建指南)
3. [CI/CD 方案](#3-cicd-方案)
4. [应用发布指南](#4-应用发布指南)
5. [常见问题排查](#5-常见问题排查)
6. [运维监控建议](#6-运维监控建议)
7. [v1.2.0 新功能说明](#7-v120-新功能说明)
8. [数据库迁移指南](#8-数据库迁移指南)
9. [v1.3.0 新功能说明](#9-v130-新功能说明)
10. [v1.4.0 新功能说明](#10-v140-新功能说明)
11. [v2.0.0 新功能说明](#11-v200-新功能说明)
12. [v2.1.0 Bug 修复说明](#12-v210-bug-修复说明)

---

## 1. 本地开发环境搭建

### 1.1 环境要求总览

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| Android Studio | Arctic Fox (2020.3.1) 或更高 | 推荐 Hedgehog (2023.1.1) |
| JDK | JDK 17（必须） | Android Studio 内置或独立安装 |
| Gradle | 8.2（由 AGP 绑定） | 建议使用 Gradle Wrapper |
| Android SDK | compileSdk 34 / minSdk 26 | 通过 Android Studio SDK Manager 安装 |
| Kotlin | 1.9.20 | 由项目插件管理 |
| Git | 2.14+ | 代码版本控制 |

### 1.2 Android Studio 安装步骤

**Step 1：下载 Android Studio**

下载地址：https://developer.android.com/studio

推荐下载 **Android Studio Hedgehog (2023.1.1)** 或最新稳定版。

**Step 2：安装后首次启动配置**

首次启动时，Android Studio 会引导配置 SDK 安装路径和 SDK 组件。确保以下组件已勾选安装：

```
☑ Android SDK Platform 34
☑ Android SDK Build-Tools 34.0.0
☑ Android SDK Command-line Tools
☑ Android Emulator（如果需要模拟器调试）
☑ Intel x86 Emulator Accelerator (HAXM)（Windows/macOS）
```

**Step 3：验证 SDK 路径**

在 Android Studio 中打开 `File → Project Structure → SDK Location`，确认 Android SDK 路径正确。

### 1.3 JDK 17 配置

**推荐使用 Android Studio 内置 JDK：**

Android Studio 捆绑的 JDK 路径示例：
- macOS：`/Applications/Android Studio.app/Contents/jbr/Contents/Home`
- Windows：`C:\Program Files\Android\Android Studio\jbr`
- Linux：`~/android-studio/jbr`

**如使用独立 JDK（推荐避免环境问题）：**

从 https://adoptium.net/ 下载 **Eclipse Temurin 17**（或 OpenJDK 17）。

设置环境变量：
```bash
# macOS / Linux (.bashrc / .zshrc)
export JAVA_HOME=/path/to/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
```

验证：
```bash
java -version
# openjdk version "17.0.x" ...
```

> ⚠️ **注意**：JDK 17 是项目硬性要求（compileOptions targetCompatibility = JavaVersion.VERSION_17）。使用 JDK 11/8 会导致编译错误。

### 1.4 必要的 Android SDK 组件

通过 `sdkmanager` 安装（位于 `<ANDROID_SDK>/cmdline-tools/latest/bin/`）：
```bash
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager "platform-tools"
```

### 1.5 推荐 IDE 插件

| 插件 | 用途 |
|------|------|
| Kotlin | Kotlin 语言支持（内置） |
| .ignore | 管理 .gitignore |
| GitToolBox | Git 增强功能 |
| ADB Idea | 快速操作 ADB |

---

## 2. 项目构建指南

### 2.1 项目结构

```
miaobi/
├── app/
│   ├── build.gradle.kts          # App 模块构建配置
│   ├── proguard-rules.pro        # ProGuard 规则（Release 构建使用）
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/miaobi/app/  # Kotlin 源代码
│       └── res/                   # 资源文件
├── build.gradle.kts              # 根构建配置（声明插件）
├── settings.gradle.kts           # 项目设置
├── gradle.properties             # Gradle 属性（AndroidX 等）
└── docs/                         # 文档目录（本文档所在）
```

**新增源码目录（v1.1.0）：**

```
app/src/main/java/com/miaobi/app/
├── data/
│   ├── local/dao/                # Room DAO
│   │   ├── StoryTemplateDao.kt   # 故事模板 DAO（新增）
│   │   └── ChapterDraftVersionDao.kt  # 章节版本 DAO（新增）
│   └── entity/                   # Room Entity
│       ├── StoryTemplate.kt      # 故事模板实体（新增）
│       └── ChapterDraftVersion.kt  # 章节版本实体（新增）
└── domain/model/
    └── StoryTemplate.kt          # 故事模板领域模型（新增）
```

**技术栈版本锁定：**

| 组件 | 版本 |
|------|------|
| Android Gradle Plugin | 8.2.0 |
| Kotlin | 1.9.20 |
| Kotlin Compiler Extension (Compose) | 1.5.5 |
| KSP | 1.9.20-1.0.14 |
| Hilt | 2.48.1 |
| Room | 2.6.1 |
| Retrofit | 2.9.0 |
| OkHttp | 4.12.0 |
| Compose BOM | 2023.10.01 |
| compileSdk / targetSdk | 34 |
| minSdk | 26 |

### 2.2 导入项目

**方法一：通过 Android Studio 导入**

1. 打开 Android Studio
2. 选择 `File → Open`
3. 选择项目根目录 `miaobi/`
4. 等待 Gradle Sync 完成（约 5-10 分钟，首次需下载依赖）

**方法二：通过命令行导入**

```bash
cd /path/to/miaobi

# 初始化 Gradle Wrapper（项目暂无 wrapper 时需要）
gradle wrapper --gradle-version=8.2

# 验证 wrapper
./gradlew -v

# 执行首次构建（下载依赖）
./gradlew assembleDebug
```

> ⚠️ **注意**：项目目前未包含 Gradle Wrapper 文件（`gradlew` 和 `gradle/wrapper/`），建议提交前执行一次 `./gradlew wrapper` 生成。

生成 wrapper 并提交：
```bash
./gradlew wrapper
git add gradlew gradle/wrapper/
git commit -m "chore: add gradle wrapper"
```

### 2.3 配置 API Key

项目使用硅基流动 API（OpenAI 兼容格式），API Key 通过本地 `local.properties` 或环境变量管理，**不硬编码在源代码中**。

**Step 1：创建 `local.properties`（已被 .gitignore 忽略，不会提交）**

```properties
# local.properties（不要提交到 Git！）
SILICONFLOW_API_KEY=your_api_key_here
```

**Step 2：在 `build.gradle.kts` 中读取（App 模块）**

项目已通过 `gradle.properties` 注入 `local.properties` 路径，但实际 API Key 读取逻辑应在代码的 `BuildConfig` 或 `DataStore` 中实现。

推荐在 `app/build.gradle.kts` 的 `defaultConfig` 中添加：
```kotlin
// app/build.gradle.kts
defaultConfig {
    applicationId = "com.miaobi.app"
    minSdk = 26
    targetSdk = 34
    versionCode = 1
    versionName = "1.0.0"

    // 从 local.properties 读取 API Key（避免硬编码）
    val apiKey = project.findProperty("SILICONFLOW_API_KEY") as String? ?: ""
    buildConfigField("String", "SILICONFLOW_API_KEY", "\"$apiKey\"")
}
```

同时在 `app/build.gradle.kts` 的 android 块中启用 buildConfig：
```kotlin
buildFeatures {
    compose = true
    buildConfig = true  // 新增这行
}
```

**Step 3：硅基流动 API Key 获取**

1. 访问 https://cloud.siliconflow.cn 注册账号
2. 进入控制台 → API Keys → 创建新 Key
3. 将 Key 填入 `local.properties` 的 `SILICONFLOW_API_KEY` 字段

**Step 4：通过环境变量注入（CI/CD 场景）**

```bash
export SILICONFLOW_API_KEY="sk-xxxxx"
./gradlew assembleDebug
```

### 2.4 构建 Debug APK

```bash
# 方式一：Android Studio UI
# Build → Build Bundle(s) / APK(s) → Build APK(s)

# 方式二：命令行
./gradlew assembleDebug
```

输出路径：
```
app/build/outputs/apk/debug/app-debug.apk
```

安装到已连接设备/模拟器：
```bash
./gradlew installDebug
```

### 2.5 构建 Release APK

Release 构建需要签名配置。在 `app/build.gradle.kts` 中添加：

```kotlin
android {
    // ... 现有配置 ...

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // 添加签名配置（密钥不要提交到 Git）
    signingConfigs {
        create("release") {
            storeFile = file("keystore/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "your_store_password"
            keyAlias = System.getenv("KEY_ALIAS") ?: "your_key_alias"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "your_key_password"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}
```

构建 Release APK：
```bash
# 通过环境变量传入密码（避免明文）
export KEYSTORE_PASSWORD="your_store_password"
export KEY_ALIAS="your_key_alias"
export KEY_PASSWORD="your_key_password"

./gradlew assembleRelease
```

输出路径：
```
app/build/outputs/apk/release/app-release.apk
```

---

## 3. CI/CD 方案

### 3.1 GitHub Actions 配置示例

在项目根目录创建 `.github/workflows/android.yml`：

```yaml
name: Android CI/CD

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
  release:
    types: [published]

env:
  JAVA_VERSION: '17'

jobs:
  lint:
    name: Lint Check
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'gradle'

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Run lint
        run: ./gradlew lint

      - name: Upload lint reports
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: lint-reports
          path: app/build/reports/lint-results-*.html

  build-debug:
    name: Build Debug APK
    runs-on: ubuntu-latest
    needs: lint
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'gradle'

      - name: Create local.properties
        run: |
          echo "sdk.dir=$ANDROID_HOME" > local.properties
          echo "SILICONFLOW_API_KEY=${{ secrets.SILICONFLOW_API_KEY }}" >> local.properties

      - name: Build Debug APK
        run: ./gradlew assembleDebug

      - name: Upload Debug APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk

  build-release:
    name: Build Release APK
    runs-on: ubuntu-latest
    if: github.event_name == 'release'
    environment: release
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: ${{ env.JAVA_VERSION }}
          distribution: 'temurin'
          cache: 'gradle'

      - name: Create local.properties
        run: |
          echo "sdk.dir=$ANDROID_HOME" > local.properties
          echo "SILICONFLOW_API_KEY=${{ secrets.SILICONFLOW_API_KEY }}" >> local.properties

      - name: Decode Keystore
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > app/release.keystore

      - name: Build Release APK
        env:
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew assembleRelease

      - name: Upload Release APK
        uses: actions/upload-artifact@v4
        with:
          name: release-apk
          path: app/build/outputs/apk/release/app-release-unsigned.apk
```

### 3.2 GitHub Secrets 配置

在 GitHub 仓库 `Settings → Secrets and variables → Actions` 中添加：

| Secret 名称 | 说明 |
|------------|------|
| `SILICONFLOW_API_KEY` | 硅基流动 API Key |
| `KEYSTORE_BASE64` | 签名 keystore 文件的 Base64 编码（`base64 release.keystore`） |
| `KEYSTORE_PASSWORD` | Keystore 密码 |
| `KEY_ALIAS` | 密钥别名 |
| `KEY_PASSWORD` | 密钥密码 |

> **注意**：v1.1.0 内置了预置的硅基流动 API Key，开箱即用。生产环境发布时仍建议使用自己申请的 Key。

### 3.3 自动化流程说明

```
Push to main ──→ Lint ──→ Build Debug APK ──→ Upload Artifact
                      │
Release Published ────┘──→ Build Release APK ──→ Upload Artifact
```

> ⚠️ **v1.1.0 CI/CD 注意事项**：GitHub Actions 中的 `SILICONFLOW_API_KEY` secret 不再是必填项（已内置默认 Key），但仍建议保留以支持用户自定义 Key 场景。


---

## 4. 应用发布指南

### 4.1 生成签名 APK

**Step 1：生成签名密钥（首次）**

```bash
keytool -genkey -v -keystore release.keystore \
  -alias miaobi -keyalg RSA -keysize 2048 -validity 10000 \
  -storepass your_store_password \
  -keypass your_key_password \
  -dname "CN=Your Name, OU=Your Org, O=Your Org, L=City, ST=State, C=CN"
```

> ⚠️ **重要**：妥善保管 `release.keystore`，丢失将无法更新已发布的应用。

**Step 2：验证密钥信息**

```bash
keytool -list -v -keystore release.keystore -alias miaobi
```

**Step 3：构建签名 APK**

配置好 `app/build.gradle.kts` 的 signingConfig 后执行：
```bash
./gradlew assembleRelease
```

**Step 4：对齐优化（可选但推荐）**

使用 `zipalign` 对齐 APK（减少内存占用）：
```bash
zipalign -v -p 4 app-release-unsigned.apk app-release-aligned.apk
```

使用 `apksigner` 签名（替代 `jarsigner`，支持 APK Signature Scheme v2/v3）：
```bash
apksigner sign \
  --ks release.keystore \
  --ks-key-alias miaobi \
  --ks-pass pass:your_store_password \
  --key-pass pass:your_key_password \
  --out app-release-signed.apk \
  app-release-unsigned.apk
```

验证签名：
```bash
apksigner verify -v app-release-signed.apk
```

### 4.2 国内应用商店发布建议

#### 酷安（推荐首选）

| 项目 | 说明 |
|------|------|
| 平台特点 | 极客用户聚集地，口碑传播效果好 |
| 注册地址 | https://www.coolapk.com/ |
| 上架要求 | 企业开发者需提供营业执照；个人开发者需实名认证 |
| 审核时长 | 通常 1-3 个工作日 |
| 特殊要求 | 需要提供应用隐私政策链接；截图不超过 5 张 |

**发布建议：**
- 先发布内测版本积累评价
- 引导用户好评（避免刷评违规）
- 持续迭代更新，保持版本活跃

#### 华为应用市场

| 项目 | 说明 |
|------|------|
| 平台特点 | 华为设备自带，应用市场权重高 |
| 注册地址 | https://developer.huawei.com/ |
| 上架要求 | 企业开发者账号（个人账号受限）；需软著或 APP 备案 |
| 审核时长 | 通常 3-7 个工作日 |
| 特殊要求 | HMS Core 兼容性（如使用华为手机需额外适配） |

**发布建议：**
- 提前准备软件著作权（建议项目立项时同步申请，约 30 个工作日）
- 准备隐私政策页面（部署在独立域名）
- 截图建议 6 寸和 10 寸平板各一套

#### 应用宝（腾讯系）

| 项目 | 说明 |
|------|------|
| 平台特点 | 微信/QQ 流量入口，用户基数大 |
| 注册地址 | https://open.qq.com/ |
| 上架要求 | 企业开发者；需软著 |
| 审核时长 | 通常 3-5 个工作日 |

#### 小米应用商店

| 项目 | 说明 |
|------|------|
| 平台特点 | MIUI 系统预装，用户活跃度高 |
| 注册地址 | https://dev.mi.com/ |
| 上架要求 | 企业/个人均可；部分分类需软著 |
| 审核时长 | 通常 2-4 个工作日 |

### 4.3 发布检查清单

```
发布前检查：
□ 确认 versionCode / versionName 已更新
□ 确认 CHANGELOG.md 已记录版本变更
□ 确认 API Key 不是 Debug 版本中的硬编码值
□ 确认隐私政策页面已部署且可访问
□ 确认应用截图和介绍文案已准备
□ 确认签名 keystore 已安全备份
□ 在测试设备完成完整功能回归测试
□ 确认应用图标和启动页符合品牌规范
□ 确认 support email / 联系方式已填写
```

---

## 5. 常见问题排查

### 5.1 编译常见错误

#### 错误 1：JDK 版本不匹配

```
error: Source option 17 is not supported with JDK 8.
```
**原因**：JDK 版本低于 17。
**解决**：安装并切换到 JDK 17，清理缓存后重新构建。
```bash
./gradlew clean
./gradlew assembleDebug
```

#### 错误 2：Gradle 和 AGP 版本不兼容

```
Plugin [id: 'com.android.application', version: '8.2.0'] was not found.
```
**原因**：Gradle 版本与 AGP 版本不匹配。AGP 8.2.0 需要 Gradle 8.2+。
**解决**：使用项目推荐的 Gradle 版本，或通过 `gradle wrapper` 升级。
```bash
./gradlew wrapper --gradle-version=8.2
```

#### 错误 3：KSP 与 Kotlin 版本不匹配

```
error: Cannot add task ':app:kspDebugKotlin' because it conflicts with the existing task.
```
**原因**：KSP 版本与 Kotlin 版本不匹配。Kotlin 1.9.20 对应 KSP 1.9.20-1.0.14。
**解决**：确认 `build.gradle.kts` 中的版本对应关系：
```kotlin
plugins {
    id("org.jetbrains.kotlin.android") version "1.9.20"
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
}
```

#### 错误 4：Compose 编译器版本不匹配

```
The compiler version (1.5.5) does not match the Kotlin compiler version (1.9.20).
```
**原因**：`kotlinCompilerExtensionVersion` 与 Kotlin 版本不匹配。
**解决**：使用 Compose 官方版本映射表中对应的版本。Kotlin 1.9.20 对应 Compose Compiler 1.5.5。

#### 错误 5：网络问题导致依赖下载失败

```
Could not resolve org.jetbrains.kotlin:kotlin-stdlib:1.9.20.
```
**原因**：国内网络访问 Google/Maven Central 困难。
**解决**：配置国内镜像。在 `settings.gradle.kts` 的 `dependencyResolutionManagement` 中添加：
```kotlin
repositories {
    google()
    mavenCentral()
    // 添加阿里云镜像
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://maven.aliyun.com/repository/google") }
}
```

#### 错误 6：Hilt 编译失败（重复定义）

```
Execution failed for task ':app:kspDebugKotlin' > Compiling... duplicate definition
```
**原因**：Hilt 注解处理器重复运行或与其他 KSP 插件冲突。
**解决**：确保 Hilt 插件在 KSP 之前应用，检查 `app/build.gradle.kts` 插件顺序。

### 5.2 AI API 连接问题

#### 问题 1：API Key 无效或未配置

```
HttpException: 401 Unauthorized
```
**排查步骤：**
1. 确认 `SILICONFLOW_API_KEY` 已正确写入 `local.properties`
2. 确认代码中 `BuildConfig.SILICONFLOW_API_KEY` 正确引用
3. 在硅基流动控制台验证 Key 是否有效
4. 检查 API 调用时 Request Header 中 Authorization 字段格式

**Request 格式（参考硅基流动 OpenAI 兼容接口）：**
```kotlin
@POST("/v1/chat/completions")
suspend fun chatCompletion(
    @Header("Authorization") authorization: String,
    @Body request: ChatCompletionRequest
): Response<ChatCompletionResponse>

// 调用时
val authHeader = "Bearer $apiKey"
```

#### 问题 2：网络超时

```
java.net.SocketTimeoutException: timeout
```
**排查步骤：**
1. 检查设备网络连接
2. 确认硅基流动 API 服务状态（https://status.siliconflow.cn/）
3. 增加 OkHttp timeout 配置：
```kotlin
OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
```

#### 问题 3：流式响应（Server-Sent Events）解析失败

```
EOFException: unexpected end of stream on okhttp3...
```
**排查步骤：**
1. 确认使用 `okhttp-sse` 处理 SSE 流
2. 检查 Content-Type 响应头是否为 `text/event-stream`
3. 使用 Charles/Fiddler 抓包分析实际响应内容

### 5.3 数据库迁移问题

#### 问题 1：Room 迁移导致数据丢失

```
IllegalStateException: Migration of database to the final version failed.
```
**原因**：Schema 变更但迁移逻辑缺失或错误。

**解决：**

方案 A — 破坏性迁移（开发阶段 / 用户可接受数据清空时）：
```kotlin
.databaseBuilder(context, AppDatabase::class.java, "miaobi.db")
    .fallbackToDestructiveMigration()
    .build()
```

方案 B — 编写迁移脚本（生产环境必须）：
```kotlin
val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE novels ADD COLUMN genre TEXT")
    }
}

.databaseBuilder(context, AppDatabase::class.java, "miaobi.db")
    .addMigrations(migration_1_2)
    .build()
```

#### 问题 2：Schema 文件缺失

```
Could not find schema writer for ...
```
**原因**：KSP 未正确生成 Schema 文件。

**解决：**
1. 在 `app/build.gradle.kts` 中配置 schema export：
```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```
2. 确保 schemas 目录存在
3. 执行 `./gradlew kspDebugKotlin` 重新生成

---

## 6. 运维监控建议

### 6.1 日志收集方案

#### 方案 A：本地日志文件（适合自建后端）

在 `app/build.gradle.kts` 中引入 `timber` 日志库：
```kotlin
implementation("com.jakewharton.timber:timber:5.0.1")
```

配置日志输出到文件：
```kotlin
class App : Application() {
    override fun onCreate() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // 生产环境：写文件
            val logDir = File(filesDir, "logs")
            val fileTree = object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    File(logDir, "app.log").appendText(
                        "${Date()} [$priority] $tag: $message\n"
                    )
                }
            }
            Timber.plant(fileTree)
        }
    }
}
```

#### 方案 B：Logcat 远程转发（适合开发调试）

使用 `adb logcat` 实时查看：
```bash
# 过滤 App 日志
adb logcat -s MiaobiTag:V *:S

# 导出日志到文件
adb logcat -d > miaobi_logcat_$(date +%Y%m%d_%H%M%S).txt

# 过滤特定错误级别
adb logcat *:E
```

#### 方案 C：接入日志服务（推荐生产环境）

推荐使用 **LogBee**（免费）或 **Firebase Crashlytics**（含日志）。

**Firebase Crashlytics 接入：**

1. 在 Firebase Console 创建项目，下载 `google-services.json`，放入 `app/` 目录
2. 在 `build.gradle.kts`（根）添加：
```kotlin
plugins {
    id("com.google.gms.google-services") version "4.4.0" apply false
}
```
3. 在 `app/build.gradle.kts` 添加：
```kotlin
plugins {
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

### 6.2 崩溃报告方案

#### 方案 A：Firebase Crashlytics（推荐）

功能：自动收集 ANR、Native 崩溃、用户日志。
免费额度：无限崩溃报告（付费项目有更多配额）。

接入步骤：参见 6.1 方案 C。

#### 方案 B：自建崩溃收集（可选）

使用 `ACRA`（Application Crash Reports for Android）：

```kotlin
// build.gradle.kts
implementation("ch.acra:acra-mail:5.11.3")

@AcraCore(reportFormat = StringFormat.JSON)
@AcraMailSender(mailTo = "dev@example.com")
@AcraToast(resText = R.string.acra_toast_text)
class App : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        ACRA.init(this)
    }
}
```

### 6.3 运维监控 CheckList

```
日常监控项：
□ 应用崩溃率（目标：< 0.5%）
□ ANR 发生率（目标：< 0.1%）
□ API 调用成功率（目标：> 99%）
□ API 平均响应时间（目标：< 3s）
□ 用户反馈（定期查看应用商店评论）

版本发布后：
□ 确认新版本崩溃率无异常上升
□ 确认 API Key 配置正确（生产环境）
□ 确认隐私政策页面可访问
□ 确认应用商店审核通过

数据安全：
□ 定期轮换 API Key（建议每 90 天）
□ 确认用户本地数据未明文存储敏感信息
□ 确认 Room 数据库未存储明文密码/Token
□ 确认日志中无敏感信息输出
```

---

## 8. 数据库迁移指南

> 本节说明 v1.0.0 → v1.1.0 的数据库 Schema 变更及迁移方案。

### 8.1 Schema 变更总览

| 操作 | 表名 | 变更说明 |
|------|------|----------|
| 新增 | `story_templates` | 故事模板表 |
| 新增 | `chapter_draft_versions` | 章节续写历史版本表 |
| 修改 | `chapter_drafts` | 新增 `is_current` 字段（Boolean，标识当前版本） |

**数据库版本：1 → 2**

### 8.2 新增表结构

#### story_templates（故事模板表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INTEGER (PK) | 自增主键 |
| `name` | TEXT | 模板名称 |
| `genre` | TEXT | 题材分类（都市/玄幻/悬疑/科幻/言情） |
| `description` | TEXT | 模板描述 |
| `prompt_template` | TEXT | AI 续写 Prompt 模板 |
| `cover_image_res` | TEXT | 封面图片资源名 |
| `sort_order` | INTEGER | 排序序号 |
| `created_at` | INTEGER | 创建时间戳（ms） |

#### chapter_draft_versions（章节续写历史版本表）

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | INTEGER (PK) | 自增主键 |
| `chapter_id` | INTEGER (FK) | 关联章节 ID |
| `content` | TEXT | 续写内容 |
| `version_number` | INTEGER | 版本序号（从 1 开始递增） |
| `created_at` | INTEGER | 创建时间戳（ms） |

#### chapter_drafts（修改）

新增 `is_current` 字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `is_current` | INTEGER (0/1) | 是否为当前版本（0=否，1=是），默认 1 |

### 8.3 Room Migration 编写

在 `AppDatabase.kt` 中添加 Migration(1, 2)：

```kotlin
val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 新增 story_templates 表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS story_templates (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                genre TEXT NOT NULL,
                description TEXT NOT NULL,
                prompt_template TEXT NOT NULL,
                cover_image_res TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        // 新增 chapter_draft_versions 表
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS chapter_draft_versions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                chapter_id INTEGER NOT NULL,
                content TEXT NOT NULL,
                version_number INTEGER NOT NULL DEFAULT 1,
                created_at INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(chapter_id) REFERENCES chapter_drafts(id) ON DELETE CASCADE
            )
        """.trimIndent())

        // chapter_drafts 新增 is_current 字段
        database.execSQL("""
            ALTER TABLE chapter_drafts
            ADD COLUMN is_current INTEGER NOT NULL DEFAULT 1
        """.trimIndent())

        // 将现有记录标记为 is_current = 1（向后兼容）
        database.execSQL("UPDATE chapter_drafts SET is_current = 1 WHERE is_current IS NULL OR is_current = 0")
    }
}
```

### 8.4 DatabaseBuilder 配置更新

```kotlin
.databaseBuilder(context, AppDatabase::class.java, "miaobi.db")
    .addMigrations(migration_1_2)  // 添加迁移
    .build()
```

### 8.5 Prepopulate 内置模板数据

v1.1.0 预置了 10 个故事模板，首次安装时需要写入数据库。使用 Room 的 `createFromAsset` 或启动时手动插入：

```kotlin
// 启动时检查并插入模板（示例逻辑）
suspend fun initDefaultTemplates(database: AppDatabase) {
    val count = database.storyTemplateDao().count()
    if (count == 0) {
        val templates = getBuiltInTemplates() // 10 个预置模板
        database.storyTemplateDao().insertAll(templates)
    }
}
```

### 8.6 版本版本上限控制

每章节最多保留 20 个历史版本。新增版本时，DAO 层应自动清理超出上限的旧版本：

```kotlin
@Query("""
    DELETE FROM chapter_draft_versions
    WHERE chapter_id = :chapterId
    AND id NOT IN (
        SELECT id FROM chapter_draft_versions
        WHERE chapter_id = :chapterId
        ORDER BY version_number DESC
        LIMIT 20
    )
""")
suspend fun trimToMaxVersions(chapterId: Long)
```

### 8.7 降级注意事项

**v1.1.0 不支持降级回退到 v1.0.0**，因为 Migration(1→2) 是单向的。如需支持降级，需要实现 Migration(2→1)（通常为空操作或数据截断），但这会导致用户升级后再卸载重装 v1.0.0 时数据丢失。

发布 v1.1.0 前确认产品策略。

---

## 7. v1.2.0 新功能说明

> 本节介绍妙笔 v1.2.0 的故事模板和续写历史版本功能。

### 7.1 故事模板（10 个预置模板）

**功能介绍：**

v1.2.0 提供 10 个预置故事模板，涵盖主流小说题材，一键套用即可开始创作。

**模板列表：**

| 序号 | 模板名称 | 题材分类 | 说明 |
|------|----------|----------|------|
| 1 | 都市传奇 | 都市 | 现代都市背景，职场/商战/日常生活 |
| 2 | 都市修真 | 玄幻 | 现代都市中的修仙者故事 |
| 3 | 仙侠世界 | 玄幻 | 古代仙侠背景，门派/飞升/神兵利器 |
| 4 | 玄幻大陆 | 玄幻 | 异世界玄幻冒险，魔法/斗气 |
| 5 | 悬疑推理 | 悬疑 | 破案/惊悚/高智商对决 |
| 6 | 刑侦迷案 | 悬疑 | 犯罪侦查，故事情节紧凑 |
| 7 | 星际科幻 | 科幻 | 太空探索/星际战争/赛博朋克 |
| 8 | 未来世界 | 科幻 | 近未来科技/人工智能/末世生存 |
| 9 | 甜蜜恋爱 | 言情 | 现代言情/甜宠/暗恋成真 |
| 10 | 古风言情 | 言情 | 古代背景/宫斗/穿越/王爷丞相 |

**技术实现：**

- 模板数据存储在 `story_templates` 表（见 §8.2）
- 创建新小说时，用户可从模板列表选择，自动填充：
  - 小说简介（prompt_template）
  - 题材分类（genre）
  - 封面配图资源（cover_image_res）
- 也支持「空白创作」，不选择任何模板

**使用方式：**

1. 首页 → 「新建小说」
2. 选择「从模板创建」或「空白创作」
3. 选择模板 → 填写小说基本信息 → 开始创作

**运维注意事项：**

- 模板数据通过 `story_templates` 表管理，可随时新增/编辑模板
- 模板变更建议通过数据库迁移（addMigration）实现
- 模板的 `prompt_template` 直接影响 AI 输出质量，需精心调试

---

### 7.2 续写历史版本（每章节最多 20 个版本）

**功能介绍：**

v1.2.0 支持保存章节续写的历史版本，每章节最多保留 20 个版本，用户可以自由切换、对比、恢复历史内容。

**功能细节：**

| 功能 | 说明 |
|------|------|
| 版本上限 | 每章节最多 20 个版本，超出后自动删除最旧版本 |
| 版本命名 | 自动编号（v1, v2, v3...），保留时间戳 |
| 版本预览 | 可查看历史版本内容，不影响当前版本 |
| 切换版本 | 可将任意历史版本恢复为当前版本 |
| 当前版本标识 | `is_current = 1` 的记录为当前版本 |

**技术实现：**

- 历史版本存储在 `chapter_draft_versions` 表（见 §8.2）
- 每次 AI 续写生成新内容时：
  1. 将当前内容保存到 `chapter_draft_versions`（如与上一版本不同）
  2. 更新 `chapter_drafts` 中的当前内容
  3. 更新 `chapter_drafts.is_current = 1`
- DAO 层通过 `trimToMaxVersions(chapterId)` 自动清理超限版本

**使用方式：**

1. 进入章节编辑页
2. 点击「历史版本」入口
3. 查看 / 预览 / 切换任意历史版本

**数据库操作示意：**

```kotlin
// 每次续写时调用
suspend fun saveNewDraft(chapterId: Long, newContent: String) {
    // 1. 保存当前版本到历史（如果内容有变化）
    val currentVersion = draftDao.getCurrentDraft(chapterId)
    if (currentVersion?.content != newContent) {
        val versionNumber = draftVersionDao.getNextVersionNumber(chapterId) + 1
        draftVersionDao.insert(
            ChapterDraftVersion(
                chapterId = chapterId,
                content = currentVersion?.content ?: "",
                versionNumber = versionNumber
            )
        )
        // 2. 清理超限版本
        draftVersionDao.trimToMaxVersions(chapterId, maxVersions = 20)
        // 3. 更新当前版本
        draftDao.updateContent(chapterId, newContent)
    }
}
```

---

## 10. v1.4.0 新功能说明

> 本节介绍妙笔 v1.4.0 的灵感生成功能。

### 10.1 灵感生成

**功能介绍：**

v1.4.0 提供灵感生成模块，帮助用户在正式续写前快速获得剧情方向建议，突破创作瓶颈。

**功能细节：**

| 功能 | 说明 |
|------|------|
| 灵感类型筛选 | 6 种灵感类型可选（剧情转折/人物塑造/世界观扩展/情感冲突/悬念设置/高潮设计） |
| 生成数量 | 每次生成 4 条剧情方向建议 |
| 选用 | 可将任意灵感方向直接应用到当前章节续写 |
| 收藏 | 支持收藏灵感方向，便于后续查阅和使用 |
| 详情 | 可查看灵感方向的完整描述和续写思路 |

**灵感类型说明：**

| 类型 | 说明 |
|------|------|
| 剧情转折 | 推动故事走向新方向的关键事件 |
| 人物塑造 | 展现角色性格或成长弧线的情节 |
| 世界观扩展 | 补充背景设定、揭示世界运行规则 |
| 情感冲突 | 角色间情感矛盾、误会或关系变化 |
| 悬念设置 | 埋下伏笔、制造悬念吸引读者 |
| 高潮设计 | 情感或情节的高强度爆发点 |

**技术实现：**

- 灵感生成通过硅基流动 API 实现，使用专用 Prompt 模板根据选定类型生成对应方向的剧情建议
- 灵感收藏数据存储在本地数据库，新增 `inspiration_favorites` 表
- 生成结果缓存到 `inspiration_cache` 表，避免重复 API 调用
- 灵感选用时，自动将选中方向的 Prompt 补充到章节续写上下文中

**数据库新增表结构：**

```kotlin
// inspiration_favorites（灵感收藏表）
data class InspirationFavorite(
    val id: Long = 0,           // 自增主键
    val title: String,          // 灵感标题
    val content: String,        // 灵感完整描述
    val inspirationType: String, // 灵感类型
    val createdAt: Long         // 收藏时间戳
)

// inspiration_cache（灵感缓存表）
data class InspirationCache(
    val id: Long = 0,           // 自增主键
    val chapterId: Long,         // 关联章节 ID
    val inspirationType: String, // 灵感类型
    val suggestions: String,     // JSON 格式的 4 条建议
    val createdAt: Long         // 生成时间戳
)
```

**使用方式：**

1. 进入章节编辑页
2. 点击「灵感」入口，进入灵感生成界面
3. 从 6 种类型中选择本次需要的灵感类型
4. 点击「生成」，获得 4 条剧情方向建议
5. 操作选项：
   - **选用**：将灵感方向应用到续写上下文，开始续写
   - **收藏**：保存灵感方向到收藏夹
   - **详情**：展开查看完整描述和续写思路

**运维注意事项：**

- 灵感生成属于额外 API 调用，用户每次生成均消耗 Token，需提醒用户适度使用
- `inspiration_cache` 表建议设置 TTL（如 7 天），自动清理过期缓存减少存储占用
- 收藏数据仅存储在本地，建议定期备份或提供导出功能防止数据丢失
- 灵感类型和 Prompt 模板可在不升级 App 的情况下通过配置热更新，建议单独管理

---

## 9. v1.3.0 新功能说明

> 本节介绍妙笔 v1.3.0 的多分支生成功能。

### 9.1 多分支生成

**功能介绍：**

v1.3.0 支持一次生成多条不同走向的续写内容，用户可在多个分支中选择最符合预期的版本采纳，或对单支不满意时单独重写。

**功能细节：**

| 功能 | 说明 |
|------|------|
| 分支数量 | 支持 2~5 条分支（可配置，默认 3 条） |
| 分支独立性 | 每条分支为独立续写，走向/风格/情节均可不同 |
| 单支采纳 | 选定任意分支后，可直接采纳为当前章节内容 |
| 单支重写 | 对单支不满意时，可单独重写该分支，其他分支保留 |
| 分支预览 | 各分支内容以卡片形式并行展示，便于对比 |

**技术实现：**

- 续写请求改为批量生成模式，通过硅基流动 API 的 `n` 参数（候选数量）实现多路续写
- 分支内容存储结构：

```kotlin
data class StoryBranch(
    val id: String,           // 分支唯一 ID
    val chapterId: Long,      // 关联章节 ID
    val content: String,      // 分支续写内容
    val branchIndex: Int,     // 分支序号（0~n-1）
    val createdAt: Long       // 创建时间戳
)
```

- 数据库新增 `story_branches` 表存储分支数据
- 分支配置（数量）通过 `StoryBranchConfig` DataStore 持久化
- 并行请求使用 `async/await` 批量收集结果，单支失败不影响其他分支展示

**使用方式：**

1. 进入章节续写界面
2. 点击「多分支」开关，设置分支数量（2~5）
3. 点击「续写」，AI 并行生成多条分支
4. 各分支以卡片形式展示，可预览内容
5. 采纳：点击「采纳」将分支内容写入章节
6. 重写：点击单支的「重写」按钮，仅重写该分支

**分支数量配置建议：**

| 场景 | 推荐分支数 |
|------|-----------|
| 快速探索（节省 Token） | 2 条 |
| 平衡选择（推荐） | 3 条 |
| 多元对比（精细创作） | 4~5 条 |

> ⚠️ **Token 消耗提示**：分支数量越多，单次续写 Token 消耗越大。建议日常使用 2~3 条，需要精细创作时再开启更多分支。

**运维注意事项：**

- 多分支生成会增加单次 API 调用量，需关注硅基流动 API 配额消耗
- 建议在硅基流动控制台设置 API 调用额度告警，防止意外超额
- `story_branches` 表数据量随使用频率增长，建议定期清理采纳后不再使用的废弃分支（DAO 层可加 TTL 清理逻辑）

---

## 11. v2.0.0 新功能说明

> 本节介绍妙笔 v2.0.0 的界面重构功能，带来全新的创作交互体验。

### 11.1 界面重构总览

v2.0.0 对写作界面进行了全面重构，新增多个交互组件，提升 AI 辅助写作的效率和体验。

**新增组件一览：**

| 组件 | 类型 | 说明 |
|------|------|------|
| WritingToolbar | 底部工具栏 | 写作常用功能快捷入口 |
| FloatingAiButton | 悬浮 AI 按钮 | 一键唤起 AI 续写 |
| RewriteStyleTabRow | 写作风格选择器 | 5 种写作风格一键切换 |
| AiContinuationPanel | AI 续写抽屉 | 从底部滑出的续写操作面板 |
| VoModeToggle | V/O 模式切换 | 切换视图模式/大纲模式 |
| 沉浸模式 | 全屏写作 | 隐藏无关界面元素，专注创作 |

---

### 11.2 WritingToolbar（底部工具栏）

**功能介绍：**

底部工具栏集成写作常用功能，包括撤销、重做、格式化、收藏等操作，无需切换界面即可快速调用。

**功能细节：**

| 功能 | 说明 |
|------|------|
| 撤销/重做 | 支持多步历史操作 |
| 格式化 | 快捷设置标题、列表、引用等格式 |
| 收藏 | 一键收藏当前章节 |
| 设置 | 快速访问写作偏好设置 |

**技术实现：**

- 使用 Compose `BottomAppBar` 或自定义 `BottomSheetScaffold` 实现
- 工具栏按钮通过 `IconButton` + `Tooltip` 提供触摸反馈
- 状态管理通过 ViewModel + StateFlow 驱动 UI 重绘
- 支持横竖屏自动隐藏/显示

---

### 11.3 FloatingAiButton（悬浮 AI 按钮）

**功能介绍：**

悬浮在编辑器角落的 AI 按钮，点击即可快速唤起 AI 续写或灵感生成，无需中断写作流程。

**功能细节：**

| 特性 | 说明 |
|------|------|
| 悬浮位置 | 默认位于右下角，支持拖拽调整 |
| 触发动作 | 点击唤起 AiContinuationPanel |
| 动画效果 | 悬停时轻微放大 + 呼吸光晕效果 |
| 可隐藏 | 沉浸模式下自动隐藏 |

**技术实现：**

- 使用 Compose `FloatingActionButton` 或自定义 `Box` + `DraggableState`
- 位置信息通过 DataStore 持久化存储
- 与 WritingToolbar 联动：工具栏展开时自动让位
- 避免遮挡编辑器光标位置（动态计算安全区域）

---

### 11.4 RewriteStyleTabRow（5 种写作风格）

**功能介绍：**

5 种预设写作风格可选，AI 根据选定风格调整输出文风，满足不同题材和场景的创作需求。

**风格列表：**

| 序号 | 风格名称 | 说明 |
|------|----------|------|
| 1 | 严谨正式 | 书面语、正式叙事，适合历史/职场 |
| 2 | 轻松日常 | 口语化、生活化，适合都市/言情 |
| 3 | 紧张刺激 | 快节奏、高张力，适合悬疑/动作 |
| 4 | 文艺抒情 | 优美细腻、情感丰富，适合言情/文艺 |
| 5 | 幽默诙谐 | 轻松搞笑、玩梗，适合轻松向小说 |

**技术实现：**

- 使用 Compose `TabRow` + `Tab` 实现风格选择器
- 选中的风格作为 Prompt 参数传递给 AI API
- 风格偏好通过 DataStore 持久化，新建章节时自动应用上次选择
- 可在 AiContinuationPanel 中直接切换，实时预览效果

---

### 11.5 AiContinuationPanel（AI 续写抽屉）

**功能介绍：**

从屏幕底部滑出的半屏抽屉面板，集成续写参数设置、分支预览、生成控制等功能。

**功能细节：**

| 区域 | 内容 |
|------|------|
| 参数区 | 写作风格（RewriteStyleTabRow）、分支数量、正文长度 |
| 预览区 | 多分支生成结果的卡片展示 |
| 操作区 | 生成/取消/采纳/重写按钮 |
| 状态区 | 生成进度、Token 消耗提示 |

**技术实现：**

- 使用 Compose `ModalBottomSheet` 或 `BottomSheetScaffold`
- 面板状态（展开/收起）通过 `ModalBottomSheetState` 管理
- 内容区域支持滚动，避免大屏设备上的内容溢出
- 与 FloatingAiButton 联动：点击按钮 → 面板滑出
- 生成进度使用 `LinearProgressIndicator` 或 `CircularProgressIndicator`

---

### 11.6 VoModeToggle（V/O 模式切换）

**功能介绍：**

切换视图模式（View）和大纲模式（Outline），满足不同创作阶段的导航需求。

**模式说明：**

| 模式 | 说明 |
|------|------|
| V 模式（View） | 沉浸式编辑器，专注当前章节写作 |
| O 模式（Outline） | 大纲视图，展示小说结构、章节列表 |

**技术实现：**

- 使用 Compose `IconToggleButton` 或 `SegmentedButton`
- 模式切换通过 ViewModel + StateFlow 驱动 UI 重组
- V/O 状态通过 DataStore 持久化，会话恢复时保持上次模式
- O 模式下使用懒加载列表（`LazyColumn`）展示章节树

---

### 11.7 沉浸模式

**功能介绍：**

全屏写作模式，隐藏状态栏、底部导航、悬浮按钮等所有干扰元素，专注纯文本创作。

**功能细节：**

| 特性 | 说明 |
|------|------|
| 全屏隐藏 | 状态栏、导航栏、工具栏、悬浮按钮全部隐藏 |
| 护眼设计 | 可选护眼色背景（米黄/深色） |
| 退出方式 | 双指从边缘滑动或点击角落退出按钮 |
| 自动保存 | 隐藏模式下仍保持自动保存，防止内容丢失 |

**技术实现：**

- 使用 `WindowInsetsController` 控制系统栏显示/隐藏
- 退出手势通过 `GestureDetector` + 边缘滑动检测实现
- 编辑器使用 `BasicTextField2` 或 `TextField` 保持轻量
- 自动保存依赖现有 `saveTimer` 机制，沉浸模式下不中断

---

### 11.8 界面交互流程

```
用户进入编辑页
    │
    ├─── 默认进入 V 模式（View）
    │        │
    │        ├─── FloatingAiButton（悬浮在右下角）
    │        │
    │        ├─── WritingToolbar（底部工具栏）
    │        │
    │        └─── 点击 FloatingAiButton ──→ AiContinuationPanel（从底部滑出）
    │                                               │
    │                                               ├─── 选择 RewriteStyleTabRow（写作风格）
    │                                               ├─── 设置分支数量
    │                                               ├─── 点击「生成」
    │                                               │        │
    │                                               │        └─── 分支预览卡片展示
    │                                               │                │
    │                                               │                ├─── 采纳 → 内容写入章节
    │                                               │                └─── 重写 → 重新生成该分支
    │                                               │
    │                                               └─── 收起面板（点击外部或下拉）
    │
    └─── 点击 VoModeToggle ──→ O 模式（Outline）
             │
             ├─── 显示小说大纲、章节列表
             └─── 点击章节 → 切换至该章节的 V 模式

全屏沉浸模式（任意入口触发）：
    │
    └─── 隐藏所有 UI → 纯编辑器
            │
            └─── 双指边缘滑动 / 点击角落退出 ──→ 恢复正常界面
```

---

### 11.9 运维注意事项

- **组件拆分**：WritingToolbar、FloatingAiButton、RewriteStyleTabRow 等组件独立开发，建议抽取为独立 Compose 文件，便于后续维护
- **状态持久化**：悬浮按钮位置、写作风格偏好、V/O 模式状态均通过 DataStore 管理，迁移时注意保留
- **Token 消耗**：AiContinuationPanel 的多分支预览会额外消耗 Token，建议在 UI 层显眼位置展示预估消耗
- **沉浸模式兼容**：部分第三方输入法在沉浸模式下有兼容问题，建议测试主流输入法（百度/讯飞/Gboard）
- **性能优化**：RewriteStyleTabRow 切换时避免重新创建 Panel，AiContinuationPanel 使用 `remember` 缓存状态

---

## 12. v2.1.0 Bug 修复说明

> 本节介绍妙笔 v2.1.0 的 P0 Bug 修复，提升产品稳定性和核心功能可用性。

### 12.1 Bug 修复总览

| Bug ID | 严重等级 | 问题描述 | 修复说明 |
|--------|----------|----------|----------|
| #1 | P0 | 假进度条 | 生成过程中显示虚假进度条，用户感知模糊 | 将进度条替换为「生成中...」文字提示，实时展示 AI 响应状态 |
| #2 | P0 | 选中文字无法触发改写 | 用户在编辑器中选中文字后，无法触发智能改写功能 | 修复文本选中逻辑与改写触发按钮的绑定，选中文字后正常唤起改写面板 |
| #3 | P0 | API Key 图标错误 | 设置页面中 API Key 输入框的图标使用错误（Visibility/VisibilityOff） | 图标统一替换为 `VisibilityOff`（隐藏态）和 `LockOpen`（已解锁可编辑态），语义更准确 |

---

### 12.2 Bug #1：假进度条 → 「生成中」文字

**问题描述：**

AI 续写生成过程中，界面显示进度条动画，但实际进度与 AI 实际生成进度无关（流式响应无法精确量化进度），导致用户误以为进度已卡住或对时间预估产生误导。

**修复方案：**

移除不准确的进度条，替换为静态文字提示「生成中...」，并在 AI 首个 token 返回后追加闪烁省略号动画（...），让用户感知生成正在进行但不产生进度误导。

**代码示例：**

```kotlin
// 修复前：假进度条
LinearProgressIndicator(
    progress = { fakeProgress },  // 与实际生成进度无关
    modifier = Modifier.fillMaxWidth()
)

// 修复后：文字提示
var dotCount by remember { mutableIntStateOf(0) }
LaunchedEffect(Unit) {
    while (isGenerating) {
        delay(500)
        dotCount = (dotCount + 1) % 4
    }
}
Text(
    text = "生成中${" .".repeat(dotCount)}",
    style = MaterialTheme.typography.bodyMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)
```

**用户体验改善：**

- 消除进度条进度与实际生成不符带来的困惑
- 省略号动画提供持续反馈，告知用户生成仍在进行
- 首个 token 返回后即显示内容，边生成边展示，降低等待焦虑

---

### 12.3 Bug #2：选中文字无法触发改写

**问题描述：**

用户在编辑器中选中一段文字后，点击「改写」按钮无响应，无法对选中内容进行 AI 改写。这是 v2.0.0 界面重构后的回归问题。

**根本原因：**

v2.0.0 重构了编辑器组件，文本选中状态（`hasSelection`）与改写按钮的 `enabled` 逻辑绑定断裂。选中文字后，`selectedText` 状态未正确传递到 AiContinuationPanel，导致改写面板无法获取选中内容。

**修复方案：**

1. 确保编辑器 TextField 的 `selection` 状态通过 `mutableStateOf<Selection>` 正确暴露
2. 在 ViewModel 中建立 `selectedText` 与 `triggerRewrite` 的联动逻辑
3. AiContinuationPanel 唤起时，优先读取 `selectedText`，若无选中则回退到全文续写

**代码示例：**

```kotlin
// ViewModel 层
private val _selectedText = mutableStateOf("")
val selectedText: String get() = _selectedText.value

fun onTextSelected(text: String) {
    _selectedText.value = text
}

fun triggerRewrite() {
    val context = if (_selectedText.value.isNotBlank()) {
        RewriteContext.Selection(_selectedText.value)
    } else {
        RewriteContext.FullChapter
    }
    _rewriteEvent.value = RewriteEvent.Start(context)
}

// 修复后：选中文字时按钮可点击
val isRewriteButtonEnabled = selectedText.isNotBlank() || hasChapterContent
IconButton(
    onClick = { viewModel.triggerRewrite() },
    enabled = isRewriteButtonEnabled
) {
    Icon(Icons.Default.Edit, contentDescription = "改写")
}
```

**回归测试要点：**

- ✅ 选中单段文字后点击改写 → 面板显示「将对选中内容进行改写」
- ✅ 未选中文字时点击改写 → 面板显示「将对全文进行续写」
- ✅ 改写完成后，选中状态应清除，内容区更新为新内容

---

### 12.4 Bug #3：API Key 图标修正

**问题描述：**

设置页面中 API Key 输入框使用了错误的图标：`Visibility`（显示态）和 `VisibilityOff`（隐藏态）。API Key 输入框的正确语义是「可编辑/已锁定」，而非「显示/隐藏密码」。

**修复方案：**

| 场景 | 修复前（错误） | 修复后（正确） |
|------|---------------|----------------|
| API Key 已填写（可编辑） | `VisibilityOff` | `LockOpen` |
| API Key 未填写（禁用态） | `Visibility` | `Lock` |

**代码示例：**

```kotlin
// 修复前（语义错误）
TextField(
    value = apiKey,
    onValueChange = { apiKey = it },
    trailingIcon = {
        Icon(
            imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff
                          else Icons.Default.Visibility,
            contentDescription = if (isApiKeyVisible) "隐藏 API Key" else "显示 API Key"
        )
    }
)

// 修复后（语义正确）
TextField(
    value = apiKey,
    onValueChange = { apiKey = it },
    trailingIcon = {
        Icon(
            imageVector = if (apiKey.isNotBlank()) Icons.Default.LockOpen
                          else Icons.Default.Lock,
            contentDescription = if (apiKey.isNotBlank()) "API Key 已填写（可编辑）"
                                  else "API Key 未填写"
        )
    }
)
```

**涉及的图标（Material Icons）：**

| 图标 | 名称 | 用途 |
|------|------|------|
| `VisibilityOff` | 隐藏密码 | ❌ 不适用于 API Key |
| `LockOpen` | 解锁/可编辑 | ✅ API Key 已填写状态 |
| `Lock` | 锁定/禁用 | ✅ API Key 未填写状态 |

---

### 12.5 运维注意事项

- **Bug #1 修复**：省去进度条组件可减少少量 UI 重绘，但核心收益是用户体验改善。建议在后续 AI SDK 升级支持精确进度时再恢复进度条
- **Bug #2 修复**：涉及编辑器核心交互逻辑，建议在主流 Android 版本（8~14）上做完整回归测试
- **Bug #3 修复**：纯 UI 修复，不涉及数据层，但注意资源文件引用变更后需同步更新所有语言版本（英语/日语等）的 contentDescription

---

## 附录

```bash
# 构建
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK（需签名配置）
./gradlew clean                  # 清理构建缓存
./gradlew kspDebugKotlin         # 仅运行 KSP（Room/Hilt 注解处理）

# 安装
./gradlew installDebug           # 安装到已连接设备
adb install -r app/build/outputs/apk/debug/app-debug.apk   # 手动安装

# 代码检查
./gradlew lint                   # 运行 Lint
./gradlew test                   # 运行单元测试
./gradlew connectedAndroidTest   # 运行 Android 单元测试（需设备）

# Gradle Wrapper
./gradlew wrapper                 # 生成 wrapper
./gradlew wrapper --gradle-version=8.4  # 升级 wrapper 版本
```

### B. 关键文件路径参考

| 文件 | 路径 |
|------|------|
| 项目根目录 | `/path/to/miaobi/` |
| 主构建配置 | `build.gradle.kts` |
| App 构建配置 | `app/build.gradle.kts` |
| Gradle 属性 | `gradle.properties` |
| API Key 配置 | `local.properties`（不提交） |
| ProGuard 规则 | `app/proguard-rules.pro` |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` |
| Release APK | `app/build/outputs/apk/release/app-release.apk` |

### C. 联系人 & 资源

| 项目 | 链接 |
|------|------|
| 硅基流动官网 | https://cloud.siliconflow.cn/ |
| 硅基流动 API 文档 | https://docs.siliconflow.cn/ |
| Android Studio 下载 | https://developer.android.com/studio |
| Jetpack Compose 文档 | https://developer.android.com/compose |
| Hilt 官方文档 | https://developer.android.com/training/dependency-injection/hilt-android |
| Room 官方文档 | https://developer.android.com/training/data-storage/room |
| Firebase Crashlytics | https://firebase.google.com/docs/crashlytics |

---

_本文档由运维 Agent 生成，适用于妙笔 AI 写小说 App v2.1.0_
