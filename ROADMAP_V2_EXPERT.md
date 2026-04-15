# 🗺️ KmpPrinter V2.0 - Master Development Roadmap
*Dokumen ini adalah cetak biru (blueprint) teknis tingkat lanjut. Dirancang khusus untuk dibaca oleh AI (seperti Gemini Flash/Pro) agar dapat mengeksekusi fitur tanpa halusinasi, kesalahan logika, atau merusak arsitektur `zero-dependency` dari KmpPrinter.*

---

## 🎯 Aturan Emas untuk AI Eksekutor (Wajib Dibaca)
1. **DILARANG MENGGUNAKAN LIBRARY EKSTERNAL**: Semua implementasi (kecuali mDNS jika terpaksa) HARUS menggunakan API bawaan platform (Android, iOS, JDK) atau standard library Kotlin.
2. **PERTahankan CONNECTOR PATTERN**: Setiap protokol baru (USB, Emulator) harus diimplementasikan dengan membuat class baru yang mewarisi/mengimplementasi interface konektor yang sudah ada (mirip dengan `AndroidBluetoothConnector`).
3. **MURNI BYTEARRAY**: Di dalam core arsitektur, printer hanya mengerti `ByteArray`. Jangan pernah mengirim String secara raw tanpa proses konversi ke byte ESC/POS.

---

## 🚀 Fase 1: Native Barcode & QR Code Engine (Prioritas Tinggi)
**Tujuan**: Menghindari rendering gambar (bitmap) untuk membuat QR/Barcode, dan menggunakan hardware printer untuk merender kode agar resolusi tajam dan cetak sekejap.

### Instruksi Implementasi untuk AI:
1. **Target File**: `printer/src/commonMain/kotlin/.../ESCPosCommandBuilder.kt`
2. **Barcode Command (Code 128)**:
   - Command HEX: `1D 6B 49 n d1...dn`
   - AI harus membuat fungsi `fun barcode(data: String): ESCPosCommandBuilder`
   - Data harus di-encode ke byte. Panjang data (n) harus dihitung otomatis oleh Kotlin.
3. **QR Code Command (Bentuk standar ESC/POS)**:
   - Mencetak QR secara native membutuhkan 4 langkah pengiriman *byte array*:
     1. Tentukan Model (1D 28 6B 04 00 31 41 n1 n2)
     2. Tentukan Ukuran Module (1D 28 6B 03 00 31 43 n) -> n = ukuran blok (1-16)
     3. Tentukan Error Correction (1D 28 6B 03 00 31 45 n) -> n = 48(L), 49(M), 50(Q), 51(H)
     4. Simpan Data ke Memory (1D 28 6B pL pH 31 50 30 d1...dk)
     5. Cetak QR (1D 28 6B 03 00 31 51 30)
   - AI harus menggabungkan ke-5 langkah ini dalam fungsi `fun qrCodeNative(data: String, size: Int = 8)`

---

## 🚀 Fase 2: Android USB OTG Connector (Pangsa Pasar POS)
**Tujuan**: Mendukung koneksi kabel langsung ke tablet kasir Android tanpa intervensi Bluetooth.

### Instruksi Implementasi untuk AI:
1. **Target Folder**: `printer/src/androidMain/kotlin/.../manager/`
2. **Buat Class**: `AndroidUsbConnector : PrinterConnector`
3. **Platform API yang digunakan**: `android.hardware.usb.UsbManager`
4. **Logika Eksekusi (Harus diikuti secara berurutan)**:
   - Dapatkan `UsbManager` dari `context.getSystemService()`.
   - Lakukan iterasi `usbManager.deviceList`. Cari perangkat dengan `interfaceClass == UsbConstants.USB_CLASS_PRINTER (7)`.
   - Minta izin runtime menggunakan `UsbManager.requestPermission()` dengan `PendingIntent`.
   - Buka koneksi via `usbManager.openDevice(device)`.
   - Temukan `UsbEndpoint` bertipe `USB_DIR_OUT`.
   - Tulis `ByteArray` perintah ESC/POS menggunakan `usbConnection.bulkTransfer(endpoint, bytes, bytes.size, timeout)`.

---

## 🚀 Fase 3: Virtual Printer Emulator (Untuk Unit Testing)
**Tujuan**: Memungkinkan developer melihat hasil cetak tanpa menggunakan kertas sungguhan. Log ke console.

### Instruksi Implementasi untuk AI:
1. **Target Folder**: `printer/src/commonMain/kotlin/.../manager/`
2. **Buat Class**: `VirtualPrinterConnector : PrinterConnector`
3. **Logika Eksekusi**:
   - Class ini tidak melakukan koneksi ke hardware apapun.
   - Saat fungsi `connect()` dipanggil, langsung kembalikan status `Success`.
   - Saat fungsi `sendData(bytes: ByteArray)` dipanggil, lakukan hal berikut:
     a. Lakukan *looping* pembedahan `ByteArray`.
     b. Jika ketemu byte `0x0A` (Line Feed), cetak "ENTER".
     c. Ubah byte hex menjadi String yang dapat dibaca manusia menggunakan representasi ASCII standard.
     d. Gunakan library logging `Napier` bawaan KmpPrinter untuk mem-print struk ke console/Logcat Android Studio dengan format kotak ASCII art.

---

## 🚀 Fase 4: Code Page & International Charset
**Tujuan**: Mencetak bahasa non-Inggris (Mandarin, Arab, Rusia) tanpa "huruf alien/kotak-kotak".

### Instruksi Implementasi untuk AI:
1. **Target File (Modifikasi)**: `PrinterConfig.kt`
   - Tambahkan property baru: `val charsetName: String = "UTF-8"` dan `val escPosCodePage: Byte = 0x00`.
2. **Target File (Modifikasi)**: `ESCPosCommandBuilder.kt`
   - Fungsi `line(text: String)` tidak boleh langsung dipanggil menggunakan `text.encodeToByteArray()`.
   - Di Kotlin KMP, AI harus menggunakan library spesifik charset jika bukan UTF-8 (atau panggil API expect/actual untuk mem-bypass platform native decoder).
   - Buat fungsi `fun selectCodePage(page: Byte)` yang mengirim byte `[0x1B, 0x74, n]`. Ini akan memberitahu memori printer fisik untuk beralih ke bahasa Mandarin/Arab sebelum teks dikirim.

---

## 🚀 Fase 5: Network Auto-Discovery (UDP Ping)
**Tujuan**: Aplikasi kasir otomatis mendeteksi alamat IP printer (misal 192.168.1.50) tanpa kasir harus mengetiknya.

### Instruksi Implementasi untuk AI:
1. **Peringatan Konektivitas**: iOS melarang broadcast network asal-asalan demi privasi, sehingga pendekatan terbaik untuk KMP adalah UDP Port scan ringan.
2. **Logika UDP Broadcast**:
   - Kirim packet kosong (atau command inisiasi ESC/POS sederhana `1B 40`) ke alamat `255.255.255.255` pada port printer standard `9100`.
   - Buat coroutine pendengar (listener socket). Jika ada perangkat keras di jaringan WiFi yang merespon (ACK), catat IP tersebut.
   - Kembalikan IP ke Flow `PrinterDevice(name="Network Printer", address="192.168.1.xx")`.
3. **Native Impl**: AI dilarang menggunakan Ktor jika memungkinkan. Gunakan Expect/Actual `java.net.DatagramSocket` untuk JVM/Android, dan `Network.framework` untuk iOS.

---
*End of Blueprint. AI, please read strictly before acting.*
