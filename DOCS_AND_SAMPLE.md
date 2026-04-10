# 🖨️ NggaPrinter: Developer Reference Guide

A professional guide to integrating and mastering thermal printing in KMP.

👉 **Looking for setup instructions?** See [INSTALLATION.md](./INSTALLATION.md).

---

## 🛠️ 1. Architecture: The Connector Pattern

NggaPrinter uses a factory-based **Connector Pattern**. This decouples the discovery and transmission logic from the main application facade.

### Key Components
- **`NggaPrinter`**: The high-level facade for printing receipts and raw bytes.
- **`PrinterConnectorFactory`**: The platform-native engine for finding and creating connectors.
- **`PrinterConnector`**: The interface representing a single active session.

---

## 📝 2. Professional Commands (Builder)

Use `newCommandBuilder(config)` to access top-tier formatting tools:

```kotlin
builder.initialize()
    .alignCenter()
    .setBold(true)
    .line("OFFICIAL RECEIPT")
    .setBold(false)
    .divider()
    // Multi-column table with weights [Item:2, Qty:1, Price:1]
    .tableRow(listOf("Spicy Ramen", "1x", "45.000"), listOf(2, 1, 1))
    .tableRow(listOf("Iced Tea", "2x", "10.000"), listOf(2, 1, 1))
    .divider()
    .invert(true) // Professional "Reverse" style
    .line(" THANK YOU ")
    .invert(false)
    .cutPaper()
```

---

## 🖼️ 3. Advanced Image Dithering

Printing photos on thermal paper often looks "blochy". NggaPrinter now defaults to **Floyd-Steinberg Dithering** in `BitmapToEscPos` (Android), ensuring smooth gradients for logos and profile pictures.

---

## 📡 4. Device Discovery

Discovery is reactive and flow-based. It provides real-time logs for better debugging.

```kotlin
val flow = printer.discovery("BLUETOOTH", DiscoveryConfig(showVirtualDevices = true)) { log ->
    println("Scan status: $log")
}
```

---

## ⚠️ 5. Platform-Specific Setup

### Android
- Requires `FINE_LOCATION` and `BLUETOOTH_SCAN` (Android 12+).
- Use `PrinterPermissionManager` helper to handle results.

### iOS
- Uses **CoreBluetooth (BLE)**.
- Ensure `Info.plist` has `NSBluetoothAlwaysUsageDescription`.
- The library automatically handles service discovery (FF00/FF01 standards).

---

Developed with ❤️ by **Ringga**
