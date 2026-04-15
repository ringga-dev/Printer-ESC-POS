# 💎 KmpPrinter V2.0 New Features Documentation

Welcome to KmpPrinter V2.0! This version introduces several advanced features designed for professional printing needs and cross-platform (KMP) expert development.

---

## 🚀 1. Native Barcode & QR Engine
Instead of rendering barcodes as low-resolution images (which often break), V2.0 uses original hardware commands.
*   **Barcode (Code 128)**: Sharp, responsive, and high-contrast.
*   **QR Code**: Follows the 5-step hardware sequence (`GS ( k`) supported by modern thermal printers.

```kotlin
builder.barcode("KMP-V2-2026")
       .qrCodeNative("https://github.com/ringga-dev", size = 8, center = true)
```

## 🔌 2. Android USB OTG Support
You can now print directly via USB cable without relying on Bluetooth.
*   **Auto-Detection**: Scans for USB printers attached to the smartphone/tablet.
*   **Reliability**: Data transmission is more stable and faster than wireless protocols.

## 🖼️ 3. Logical Preview Engine (New!)
This feature allows you to display a "Mirror" of the receipt in your app before actually printing it.
*   **PreviewBlock**: A list of objects (Text, Barcode, QR) representing the content.
*   **Flexible Rendering**: You can draw this preview using Jetpack Compose, SwiftUI, or plain text.

```kotlin
// Get logical blocks for UI preview
val previewBlocks = builder.buildPreview()

// Render in UI
previewBlocks.forEach { block ->
    when(block) {
        is PreviewBlock.Text -> MyTextComponent(block)
        is PreviewBlock.QRCode -> MyQRComponent(block)
    }
}
```

## ⚖️ 4. Auto-Centering & Margins
Ticket calibration is now much easier.
*   **Left Margin (Dots)**: Offset the entire print area to the right for imprecise hardware.
*   **Auto Center**: Automatically calculates the effective print area so that left and right margins are always equal (symmetrical).

## 🌍 5. International Charset (Expect/Actual)
Supports printing non-Latin languages without external libraries.
*   **Standard Encoders**: Supports UTF-8, GBK (Mandarin), BIG5, Windows-1252.
*   **Enum Safety**: Use `PrinterCharset.GBK` to avoid naming errors.

## 📡 6. Network Auto-Discovery
Reactive detection of local network printers using UDP Broadcast.
*   Call `printer.discovery("NETWORK")` and get the IP list automatically.

## 🔐 7. Library-Level Permission Management
No more headaches with Android 12+ permissions (Bluetooth/Location).
*   Unified entry point: `printer.checkAndRequestPermissions("BLUETOOTH") { granted -> ... }`

## 🖥️ 8. Virtual Printer Emulator
Debug without wasting paper.
*   Logcat displays receipts in a clean **ASCII Art** format.
*   Visually detects Barcode, QR, and Line Feeds in the console log.

---

Developed with ❤️ by **Ringga Dev**
