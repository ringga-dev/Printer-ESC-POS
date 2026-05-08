# Printer-ESC-POS V2.1: 企业级版本深度探索

欢迎阅读工业级 Kotlin 多平台热敏打印权威指南。

## 📊 平台功能矩阵

| 功能 | Android | iOS | 桌面端 (JVM) | Web (WASM) |
| :--- | :---: | :---: | :---: | :---: |
| **连接方式** | | | | |
| - 经典蓝牙 | ✅ | ❌ | ❌ | ✅ |
| - 低功耗蓝牙 (BLE) | ✅ | ✅ | ❌ | ✅ |
| - USB OTG / 直连 | ✅ | ❌ | ✅ | ✅ |
| - 网络 (TCP) | ✅ | ✅ | ✅ | ✅ |
| **加固特性** | | | | |
| - 线程锁 (Mutex) | ✅ | ✅ | ✅ | ✅ |
| - 分块发送 | ✅ | ✅ | ✅ | ✅ |
| **高级图像处理** | | | | |
| - Floyd-Steinberg | ✅ | ✅ | ✅ | ✅ |
| - Atkinson 抖动 | ✅ | ✅ | ✅ | ✅ |
| - 图像旋转 | ✅ | ✅ | ✅ | ✅ |
| **格式化支持** | | | | |
| - 硬件级条码/QR | ✅ | ✅ | ✅ | ✅ |
| - 页面模式 (XY布局)| ✅ | ✅ | ✅ | ✅ |
| - PDF/原生渲染 | ✅ | ⏳ | ✅ | ⏳ |

---

## 🛡️ 企业级加固 (V2.1)

在高流量的零售环境中，硬件的稳定性至关重要。V2.1 引入了两大核心稳定性机制：

### 1. 基于 Mutex 的并发保护
所有连接器现在都继承自 `BasePrinterConnector`，它实现了一个集中的 **Mutex (互斥锁)**。
- **优势**: 如果多个协程尝试同时打印，库会自动将它们排队处理。不再会出现数据损坏或“打印机繁忙”的错误。

### 2. 分块传输控制 (Flow Control)
廉价的热敏打印机通常接收缓冲区非常小。发送大图像可能会导致打印机卡死。
- **机制**: 数据以 **512 字节分块** 发送，每块之间有 **20 毫秒** 的微小延迟。
- **优势**: 显著提高了在入门级蓝牙打印机上的打印可靠性。

---

## 🚀 如何使用 (代码示例)

### 1. 便捷的一行代码集成 (Printer DSL)
使用 `print` 扩展函数以获得最佳的开发体验。

```kotlin
// Android, iOS, 或 JVM
val printer = KmpPrinter() 
val config = PrinterConfig(...)

printer.print(config) {
    initialize()
    alignCenter()
    text("Hello Enterprise")
    line("V2.1 加固版")
    feed(3)
    cut()
}
```

### 2. 高级图像处理
使用高性能抖动算法获得更高质量的 Logo 打印效果。

```kotlin
printer.print(config) {
    imageAdvanced(
        pixels = logoPixels,
        width = 384,
        height = 200,
        dithering = "FLOYD" // 选项: "THRESHOLD", "FLOYD", "ATKINSON"
    )
}
```

### 3. 页面模式 (XY 坐标定位)
适用于复杂的布局，当逐行打印无法满足需求时使用。

```kotlin
builder.enterPageMode()
    .setPagePrintArea(0, 0, 384, 500)
    .setAbsoluteHorizontalPosition(100)
    .text("坐标 X=100")
    .setPageVerticalPosition(50)
    .text("坐标 Y=50")
    .printPageAndReturn()
```

### 4. 直接打印 PDF (仅限 Android/JVM)
```kotlin
val pdfData: ByteArray = ... 
printer.printPdf(config, pdfData)
```

---

## 🔧 自定义连接器实现
如果您有特殊的硬件接口，可以通过扩展 `BasePrinterConnector` 来创建自定义连接器：

```kotlin
class MySpecialConnector : BasePrinterConnector() {
    override suspend fun connect(config: PrinterConfig): Boolean = ...
    override suspend fun sendRawData(data: ByteArray): Boolean = ...
    override suspend fun readData(count: Int, timeout: Long): ByteArray? = ...
    override suspend fun disconnect() = ...
    override fun isConnected(): Boolean = ...
}
```

---

*如需查看详细的安装步骤，请参阅 [INSTALLATION_ZH.md](./INSTALLATION_ZH.md)。*

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
