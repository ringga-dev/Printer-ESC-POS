# 📜 Changelog

All notable changes to this project will be documented in this file.

## [2.0.0] - 2026-05-08 (Enterprise SDK Release)

### Added
- **iOS Perfection**: Full support for real TCP Network printing via POSIX Sockets and refined BLE connectivity with Delegate Pattern.
- **Real-time Status Monitoring**: Implementation of `PrinterStatusMonitor` for iOS and Android to detect paper-out, cover-open, and other hardware states.
- **Advanced Image Processing**: New `ImageScaler` with Nearest-Neighbor and Bilinear interpolation for professional logo rendering.
- **Web (Wasm/JS) Foundation**: Introduced `WasmJSBridge` for WebBluetooth, WebUSB, and Web Serial API support.
- **Platform Perfection**: Standardized error handling and performance optimizations across Android, JVM, iOS, and Web.

---

## [1.0.3] - 2026-04-15

### Added
- **Native Barcode & QR Engine**: Support for high-resolution hardware-rendered QR Codes (standard 5-step sequence) and Barcodes (Code 128).
- **Android USB OTG Support**: New `AndroidUsbConnector` for direct wired printing on Android devices.
- **Logical Preview Engine**: `buildPreview()` method in `ESCPosCommandBuilder` to generate UI-ready blocks for in-app receipt previews.
- **Virtual Printer Emulator**: `VirtualPrinterConnector` for debugging without physical paper, outputs ASCII art to logs.
- **International Charset Support**: Added `selectCodePage` and improved charset handling for non-Latin characters.
- **Network Auto-Discovery**: Reactive scanning for network printers via UDP broadcast.
- **Permission Management**: Simplified Android 12+ permission handling with `PrinterPermissionManager`.
- **Auto-Centering & Margins**: Dynamic calculation of printable area for improved layout precision.

### Updated
- Improved `ESCPosCommandBuilder` with extensive styling and formatting options.
- Enhanced robustness in Bluetooth and Network connectors.
- Comprehensive documentation updates for V2-level features.

---

## [1.0.2] - Previous Release
- Initial stable release with basic Bluetooth and Network support.
