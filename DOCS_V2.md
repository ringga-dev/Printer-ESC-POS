# 💎 Dokumentasi Fitur Baru KmpPrinter-ESC-POS V2.0: Panduan Teknis Enterprise Edition

Selamat datang di panduan definitif untuk pencetakan thermal Kotlin Multiplatform kelas industri.

## 📊 Matriks Fitur Platform

| Fitur | Android | iOS | Desktop (JVM) | Web (WASM) |
| :--- | :---: | :---: | :---: | :---: |
| **Konektivitas** | | | | |
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

Dalam lingkungan retail dengan trafik tinggi, stabilitas hardware adalah segalanya. V2.1 memperkenalkan dua mekanisme stabilitas inti:

### 1. Mutex-based Concurrency Protection
Semua konektor kini mewarisi `BasePrinterConnector`, yang mengimplementasikan **Mutex** terpusat.
- **Manfaat**: Jika beberapa coroutine mencoba mencetak secara bersamaan, library akan memasukkannya ke antrean secara otomatis. Tidak ada lagi data yang rusak atau error "Printer Busy".

### 2. Chunked Flow Control
Printer thermal murah seringkali memiliki buffer penerima yang sangat kecil. Mengirim gambar besar bisa menyebabkan printer "hang".
- **Mekanisme**: Data dikirim dalam **chunk 512-byte** dengan jeda singkat **20ms** di antaranya.
- **Manfaat**: Meningkatkan keandalan secara drastis pada printer Bluetooth ekonomis.

---

## 🚀 Cara Menggunakan (Contoh Kode)

### 1. Integrasi Satu Baris (Printer DSL)
Gunakan ekstensi `print` untuk pengalaman pengembangan terbaik.

```kotlin
// Android, iOS, atau JVM
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

### 2. Pemrosesan Gambar Lanjutan
Kualitas logo lebih baik dengan algoritma dithering berperforma tinggi.

```kotlin
printer.print(config) {
    imageAdvanced(
        pixels = logoPixels,
        width = 384,
        height = 200,
        dithering = "FLOYD" // Opsi: "THRESHOLD", "FLOYD", "ATKINSON"
    )
}
```

### 3. Page Mode (XY Positioning)
Untuk tata letak kompleks di mana pendekatan baris-demi-baris tidak mencukupi.

```kotlin
builder.enterPageMode()
    .setPagePrintArea(0, 0, 384, 500)
    .setAbsoluteHorizontalPosition(100)
    .text("Koordinat X=100")
    .setPageVerticalPosition(50)
    .text("Koordinat Y=50")
    .printPageAndReturn()
```

### 4. Pencetakan PDF Langsung (Android/JVM Saja)
```kotlin
val pdfData: ByteArray = ... 
printer.printPdf(config, pdfData)
```

---

## 🔧 Implementasi Konektor Kustom
Jika Anda memiliki hardware khusus, Anda dapat membuat konektor sendiri dengan mewarisi `BasePrinterConnector`:

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

*Untuk langkah instalasi detail, lihat [INSTALLATION.md](./INSTALLATION.md).*

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
