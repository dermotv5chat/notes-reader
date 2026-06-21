# Debug 与 Release 构建

本文说明本项目的 Debug / Release 包差异，以及 **不要在 Debug 上开启代码压缩（minify）** 的原因。

## 日常用的是哪种包？

| 命令 / 脚本 | 构建类型 | APK 路径 |
|-------------|----------|----------|
| `.\gradlew assembleDebug` | **Debug** | `app/build/outputs/apk/debug/app-debug.apk` |
| `.\install2device.ps1` | **Debug**（内部调用 `assembleDebug`） | 同上，并 `adb install -r` |
| `.\gradlew assembleRelease` | **Release** | `app/build/outputs/apk/release/app-release.apk` |

自己开发、装手机试用，一直用 **Debug** 即可。

## Debug 与 Release 有何不同？

| | Debug | Release |
|---|-------|---------|
| **用途** | 开发、自测、断点调试 | 正式分发、长期自用、上架 |
| **可调试** | 是（`debuggable=true`） | 否 |
| **签名** | Android 自动 debug 证书 | 需配置 keystore（项目尚未配置正式签名） |
| **与另一类型互装** | 签名不同，不能直接覆盖安装 | 同左，需先卸载再装 |
| **当前 minify** | 默认关闭 | [`app/build.gradle.kts`](../app/build.gradle.kts) 中 `isMinifyEnabled = false` |

## 运行性能有差别吗？

**在当前项目配置下，几乎感觉不到差别。**

Release 与 Debug 跑的是同一套业务代码；Release 也未开启 R8 压缩/混淆。TTS、网络同步、列表滚动等主要耗时在系统和 I/O，与 build type 关系不大。

若将来在 **Release** 上开启 `isMinifyEnabled = true`，可能带来略小的 APK 和轻微启动优化，但需要补全 ProGuard 规则并充分测试；与 Debug 是否开 minify 无关。

## 不要在 Debug 上开 `isMinifyEnabled = true`

### `isMinifyEnabled` 会做什么？

设为 `true` 时，R8 会：

1. **Shrink**：删除未使用的类、方法
2. **Obfuscate**：混淆类名、方法名
3. **Optimize**：优化字节码

若在 `buildTypes { debug { ... } }` 里加上：

```kotlin
debug {
    isMinifyEnabled = true
    proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
    )
}
```

构建仍可能成功，但 Debug 包的行为会明显变差，且在本项目中 **很容易运行时崩溃**。

### 对本项目的具体风险

[`proguard-rules.pro`](../app/proguard-rules.pro) 目前几乎为空，而 App 依赖：

- **Hilt**（依赖注入、反射）
- **Retrofit + Gson**（接口与 JSON 模型）
- **Jetpack Compose / Navigation**
- **EncryptedSharedPreferences** 等

R8 可能误删或混淆「静态分析认为没用、运行时必需」的类，典型现象：

- 启动即崩溃
- GitHub 同步失败（Gson 模型被混淆）
- Hilt 注入失败
- TTS / 前台服务异常

Release 若要开 minify，也需要为上述库编写大量 `-keep` 规则；当前 Release 仍为 `false`，说明项目 **尚未为混淆做过验证**。

### 对开发体验的影响

Debug 的设计目标是便于调试。开启 minify 后：

- 崩溃堆栈变成 `a.b.c` 等混淆名，难以对照源码
- 断点、单步调试可能错位
- 每次构建多跑 R8，**编译明显变慢**

业界惯例是：**Debug 关闭 minify，仅在 Release 且规则就绪后再考虑开启。**

### 小结

| 目标 | 建议 |
|------|------|
| 日常装手机、改代码 | 继续 `assembleDebug` / `install2device.ps1` |
| 测 Release 体积或性能 | 用 `assembleRelease`，不要改 Debug |
| 缩小包体、正式分发 | 在 **Release** 开 minify + 补 ProGuard 规则 + 全量测试 |

**结论：不要在 Debug 上开 `isMinifyEnabled = true`——编译更慢、难调试，且在本项目现有配置下极易引入运行时错误，几乎没有值得的收益。**

## 相关文件

- [`app/build.gradle.kts`](../app/build.gradle.kts) — `buildTypes` 配置
- [`app/proguard-rules.pro`](../app/proguard-rules.pro) — ProGuard / R8 保留规则
- [`install2device.ps1`](../install2device.ps1) — 构建并安装 Debug 包
- [install-to-device.md](install-to-device.md) — 装机步骤
