# Printer-ESC-POS V2.1: Enterprise Edition Deep-Dive

Welcome to the definitive guide for industrial-grade Kotlin Multiplatform Thermal Printing.

## 📊 Platform Feature Matrix

| Feature | Android | iOS | Desktop (JVM) | Web (WASM) |
| :--- | :---: | :---: | :---: | :---: |
| **Connectivity** | | | | |
| - Bluetooth (Classic) | ✅ | ❌ | ❌ | ✅ |
| - Bluetooth LE (BLE) | ✅ | ✅ | ❌ | ✅ |
| - USB OTG / Direct | ✅ | ❌ | ✅ | ✅ |
| - Network (TCP) | ✅ | ✅ | ✅ | ✅ |
| **Hardening** | | | | |
| - Mutex Locking | ✅ | ✅ | ✅ | ✅ |
| - Chunked Sending | ✅ | ✅ | ✅ | ✅ |
| **Advanced Imaging** | | | | |
| - Floyd-Steinberg | ✅ | ✅ | ✅ | ✅ |
| - Atkinson Dithering | ✅ | ✅ | ✅ | ✅ |
| - Image Rotation | ✅ | ✅ | ✅ | ✅ |
| **Formatting** | | | | |
| - Hardware QR/Barcode| ✅ | ✅ | ✅ | ✅ |
| - Page Mode (XY) | ✅ | ✅ | ✅ | ✅ |
| - PDF/Native Render | ✅ | ⏳ | ✅ | ⏳ |

---

## 🛡️ Enterprise Hardening (V2.1)

In high-traffic retail environments, hardware reliability is king. V2.1 introduces two core stability mechanisms:

### 1. Mutex-based Concurrency Protection
All connectors now inherit from `BasePrinterConnector`, which implements a centralized **Mutex**. 
- **Benefit**: If multiple coroutines attempt to print simultaneously, the library automatically queues them. No more corrupted data or "Printer Busy" errors.

### 2. Chunked Flow Control
Cheap thermal printers often have very small receive buffers. Sending a large image can cause them to freeze. 
- **Mechanism**: Data is sent in **512-byte chunks** with a tiny **20ms delay** between them.
- **Benefit**: Dramatically increases reliability on budget Bluetooth printers.

---

## 🚀 How to Use (Code Examples)

### 1. Easy One-Liner Integration
Use the `print` extension for the best developer experience.

```kotlin
// Android, iOS, or JVM
val printer = KmpPrinter() 
val config = PrinterConfig(...)

printer.print(config) {
    initialize()
    alignCenter()
    text("Hello Enterprise")
    line("V2.1 Hardened")
    feed(3)
    cut()
}
```

### 2. Advanced Image Processing
Better quality logos using high-performance dithering.

```kotlin
printer.print(config) {
    imageAdvanced(
        pixels = logoPixels,
        width = 384,
        height = 200,
        dithering = "FLOYD" // Options: "THRESHOLD", "FLOYD", "ATKINSON"
    )
}
```

### 3. Page Mode (XY Positioning)
For complex layouts where line-by-line is not enough.

```kotlin
builder.enterPageMode()
    .setPagePrintArea(0, 0, 384, 500)
    .setAbsoluteHorizontalPosition(100)
    .text("Coordinate X=100")
    .setPageVerticalPosition(50)
    .text("Coordinate Y=50")
    .printPageAndReturn()
```

### 4. Direct PDF Printing (Android/JVM Only)
```kotlin
val pdfData: ByteArray = ... 
printer.printPdf(config, pdfData)
```

---

## 🔧 Custom Connector Implementation
If you have a specialized hardware interface, you can create a custom connector by extending `BasePrinterConnector`:

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

*For detailed installation steps, see [INSTALLATION_EN.md](./INSTALLATION_EN.md).*
