# 🖨️ NggaPrinter: Developer Reference Guide

A professional guide to integrating and mastering thermal printing in KMP.

---

## 🛠️ 1. Architecture: The Connector Pattern

NggaPrinter uses a factory-based **Connector Pattern**. This decouples the discovery and transmission logic from the main application facade.

### Key Components
- **`NggaPrinter`**: The high-level facade for printing receipts and raw bytes.
- **`PrinterConnectorFactory`**: The platform-native engine for finding and creating connectors.
- **`PrinterConnector`**: The interface representing a single active session (Bluetooth, USB, or Network).

---

## 📡 2. Device Discovery

Discovery is reactive and flow-based. It provides real-time logs for better debugging.

```kotlin
val flow = printer.connectorFactory.discovery("USB") { log ->
    // Display this in a status bar or log panel
    println("Scan status: $log")
}

val devices = flow.first() // Or collect it in UI
```

---

## 📝 3. Precision Layouts

The `ESCPosTextLayout` engine handles string padding and wrapping to ensure your receipts look identical on different hardware.

### Best Practices:
1.  **58mm Paper**: Use **32 CPL** (Characters Per Line). Standard for compact mobile printers.
2.  **80mm Paper**: Use **42-48 CPL**. Standard for kitchen and POS desktop printers.
3.  **Encoding**: The library uses standard System B codes. Ensure your printer is set to **CodePage 0 (PC437)** for maximum compatibility.

---

## 💰 4. Accounting & Fiscal Support

The `ReceiptData` model is built for professional fiscal requirements.

| Field | Description | Renders as |
| :--- | :--- | :--- |
| `taxAmount` | VAT / Pajak | Automated breakdown row |
| `discountAmount` | Global discount | Strikethrough-style row |
| `verificationUrl` | Fiscal verification | High-density **QR Code** |
| `transactionId` | Unique ID | **CODE128 Barcode** |

---

## ⚠️ Platform-Specific Setup

### Android
- Bluetooth scanning requires `FINE_LOCATION` and `BLUETOOTH_SCAN`.
- USB requires permission intent if vendor isn't registered in `device_filter`.

### iOS
- Uses CoreBluetooth (BLE). Ensure the printer supports the BLE peripheral profile.

### JVM
- Network (TCP) is the most stable for JVM desktop apps. USB support requires specific driver matching.

---

Developed with ❤️ by **Ringga**
