# 💎 KmpPrinter V2.0 新功能文档

欢迎使用 KmpPrinter V2.0！此版本引入了多项高级功能，专为专业打印需求和跨平台 (KMP) 专家开发设计。

---

## 🚀 1. 原生条码与二维码引擎
不同于将条码渲染为低分辨率图像（通常会模糊），V2.0 使用原始硬件指令。
*   **条码 (Code 128)**: 锐利、响应迅速且对比度高。
*   **二维码**: 遵循现代热敏打印机支持的 5 步硬件序列 (`GS ( k`)。

```kotlin
builder.barcode("KMP-V2-2026")
       .qrCodeNative("https://github.com/ringga-dev", size = 8, center = true)
```

## 🔌 2. Android USB OTG 支持
您现在可以通过 USB 线直接打印，无需依赖蓝牙。
*   **自动检测**: 扫描连接到智能手机/平板电脑的 USB 打印机。
*   **可靠性**: 数据传输比无线协议更稳定、更快速。

## 🖼️ 3. 逻辑预览引擎 (新增!)
此功能允许您在实际打印之前在应用中显示小票的“镜像”。
*   **PreviewBlock**: 代表内容的对象列表（文本、条码、二维码）。
*   **灵活渲染**: 您可以使用 Jetpack Compose、SwiftUI 或纯文本绘制此预览。

```kotlin
// 获取 UI 预览的逻辑块
val previewBlocks = builder.buildPreview()

// 在 UI 中渲染
previewBlocks.forEach { block ->
    when(block) {
        is PreviewBlock.Text -> MyTextComponent(block)
        is PreviewBlock.QRCode -> MyQRComponent(block)
    }
}
```

## ⚖️ 4. 自动居中与页边距
票据校准现在变得更加容易。
*   **左边距 (Dots)**: 为不精确的硬件向右偏移整个打印区域。
*   **自动居中**: 自动计算有效打印区域，使左右边距始终相等（对称）。

## 🌍 5. 国际字符集 (Expect/Actual)
支持无需外部库即可打印非拉丁语言。
*   **标准编码器**: 支持 UTF-8, GBK (中文简体), BIG5 (中文繁体), Windows-1252。
*   **枚举安全**: 使用 `PrinterCharset.GBK` 以避免命名错误。

## 📡 6. 网络自动发现
使用 UDP 广播响应式检测本地网络打印机。
*   调用 `printer.discovery("NETWORK")` 即可自动获取 IP 列表。

## 🔐 7. 库级权限管理
不再为 Android 12+ 权限（蓝牙/位置）感到头疼。
*   统一入口点：`printer.checkAndRequestPermissions("BLUETOOTH") { granted -> ... }`

## 🖥️ 8. 虚拟打印机仿真器
无需浪费纸张即可进行调试。
*   Logcat 以整洁的 **ASCII 艺术** 格式显示小票。
*   在控制台日志中直观检测条码、二维码和换行符。

---

Developed with ❤️ by **Ringga Dev**
