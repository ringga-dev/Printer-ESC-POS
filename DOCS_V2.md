# 💎 Dokumentasi Fitur Baru KmpPrinter V2.0

Selamat datang di KmpPrinter V2.0! Versi ini memperkenalkan banyak fitur tingkat lanjut untuk kebutuhan cetak profesional dan pengembangan lintas platform (KMP).

---

## 🚀 1. Native Barcode & QR Engine
Alih-alih merender barcode sebagai gambar (yang seringkali pecah), V2.0 menggunakan perintah hardware asli.
*   **Barcode (Code 128)**: Responsif dan tajam.
*   **QR Code**: Mengikuti standar 5-langkah hardware (`GS ( k`) yang didukung printer modern.

```kotlin
builder.barcode("KMP-V2-2026")
       .qrCodeNative("https://github.com/ringga-dev", size = 8, center = true)
```

## 🔌 2. Android USB OTG Support
Sekarang Anda bisa mencetak langsung menggunakan kabel USB tanpa perlu Bluetooth.
*   **Auto-Detection**: Mendeteksi printer USB yang terpasang di smartphone/tablet.
*   **Reliability**: Pengiriman data lebih stabil dan cepat dibanding nirkabel.

## 🖼️ 3. Logical Preview Engine (New!)
Fitur ini memungkinkan Anda menampilkan "Bayangan" struk di aplikasi sebelum benar-benar mencetaknya.
*   **PreviewBlock**: Daftar objek (Teks, Barcode, QR) yang mewakili isi struk.
*   **Flexible Rendering**: Anda bisa menggambar preview ini menggunakan Jetpack Compose, SwiftUI, atau teks biasa.

```kotlin
// Ambil daftar blok untuk preview UI
val previewBlocks = builder.buildPreview()

// Gunakan di UI
previewBlocks.forEach { block ->
    when(block) {
        is PreviewBlock.Text -> MyTextComponent(block)
        is PreviewBlock.QRCode -> MyQRComponent(block)
    }
}
```

## ⚖️ 4. Auto-Centering & Margins
Kalibrasi struk kini jauh lebih mudah.
*   **Left Margin (Dots)**: Geser seluruh area cetak ke kanan untuk hardware yang tidak presisi.
*   **Auto Center**: Jika aktif, sistem akan menghitung lebar area cetak secara otomatis agar margin kiri dan kanan selalu sama (simetris).

## 🌍 5. International Charset (Expect/Actual)
Mendukung cetak bahasa non-Latin tanpa library eksternal.
*   **Standard Encoders**: Mendukung UTF-8, GBK (Mandarin), BIG5, Windows-1252.
*   **Enum Safety**: Gunakan `PrinterCharset.GBK` untuk menghindari kesalahan penulisan.

## 📡 6. Network Auto-Discovery
Mendeteksi printer di jaringan lokal secara reaktif menggunakan UDP Broadcast.
*   Panggil `printer.discovery("NETWORK")` dan dapatkan daftar IP printer secara otomatis.

## 🔐 7. Library-Level Permission Management
Jangan pusing lagi dengan izin Android 12+ (Bluetooth/Location).
*   Gunakan fungsi satu-pintu: `printer.checkAndRequestPermissions("BLUETOOTH") { granted -> ... }`

## 🖥️ 8. Virtual Printer Emulator
Debugging tanpa membuang kertas.
*   Logcat akan menampilkan struk dalam format **ASCII Art** yang rapi.
*   Mendeteksi Barcode, QR, dan Line Feed secara visual di log console.

---

Developed with ❤️ by **Ringga Dev**
