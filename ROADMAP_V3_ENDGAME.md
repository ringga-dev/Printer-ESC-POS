# 🌌 KmpPrinter V3.0 - The Endgame (Enterprise Blueprint)
*Dokumen ini adalah cetak biru (blueprint) tingkat Arsitek Perangkat Lunak. Merupakan puncak evolusi dari library pencetakan KMP menuju standar Enterprise, Framework, dan Kiosk-Automated Ecosystems.*

---

## 🎯 Aturan Emas untuk Skala "Endgame"
Fitur V3.0 membutuhkan modifikasi arsitektur `core` secara mendalam. AI eksekutor dilarang mengerjakan V3.0 sebelum seluruh roadmap V2.0 (`ROADMAP_V2_EXPERT.md`) terimplementasi 100% dan lolos *unit test*.

---

## 🚀 Fase 1: Compose-to-Receipt Bridge (Kompiler Deklaratif)
**Tujuan**: Mengizinkan developer mendesain struk langsung menggunakan DSL / Syntax ala Jetpack Compose, alih-alih menggunakan `CommandBuilder` tradisional. Ini merupakan **Puncak Developer Experience (DX)**.

### Instruksi Teknis Tingkat Lanjut:
1. **DILARANG MERENDER MENJADI GAMBAR (BITMAP)**. Mengonversi UI ke gambar sebelum dicetak sangat lambat dan memakan memori OOM.
2. **Abstract Syntax Tree (AST)**:
   - Buat Hierarki Node: `interface ReceiptNode`. Kemudian buat `class TextNode(val text: String)`, `class DividerNode()`, dsb.
3. **Custom Compose Applier**:
   - Buat lingkup `@Composable` bernama `ReceiptTheme { ... }`.
   - Modifikasi `androidx.compose.runtime.Applier` agar fungsi composable tidak digambar di memori grafis Android/iOS, melainkan ditumpuk menjadi `List<ReceiptNode>`.
4. **Kompiler Bytecode**:
   - Ekstrak hierarki `ReceiptNode` tersebut dan terjemahkan secara terbalik ke dalam pemanggilan fungsi `ESCPosCommandBuilder` secara rekursif.

---

## 🚀 Fase 2: Hardware Status Diagnostics (Low-Level DLE EOT)
**Tujuan**: Menjadikan printer "berbicara" kembali ke perangkat (Two-Way Communication). Mutlak diperlukan untuk Vending Machine atau Self-Service Kiosks.

### Instruksi Teknis Tingkat Lanjut:
1. **Modifikasi Connector Layer**: Interface `PrinterConnector` yang saat ini bersifat *Fire-and-Forget* harus diubah menjadi sistem *Duplex* (Socket/Bluetooth InputStream harus terus dipantau secara non-blocking).
2. **Protokol ESC/POS Transmisi Real-Time (DLE EOT)**:
   - Command HEX: `10 04 n` (n = 1: Printer Status, 2: Offline Status, 3: Error Status, 4: Roll Paper Sensor Status).
3. **Pembedahan Bit (Bitwise Operation)**:
   - Kirim `[0x10, 0x04, 0x04]` lalu dengarkan *(listen)* 1 Byte kembalian dari perangkat keras printer.
   - Pembedahan Binary untuk Status Kertas:
     - `val isPaperNearEnd = (responseByte and 0x0C) == 0x0C` (Bit 2 & 3: 1).
     - `val isPaperEnd = (responseByte and 0x60) == 0x60` (Bit 5 & 6: 1).
4. **State Management**:
   - Ekspos hasil sensor ke aplikasi menggunakan Kotlin `StateFlow<HardwareStatus>`.

---

## 🚀 Fase 3: Cloud Print Spooler (Enterprise Hub)
**Tujuan**: Mengubah `KmpPrinter` dari library pemrosesan lokal menjadi *Agent Background* yang dapat merespons perintah cetak jarak jauh layaknya mesin EDC GoBiz / GrabFood.

### Instruksi Teknis Tingkat Lanjut:
1. **Pemisahan Modul**: Jangan letakkan di modul `:printer`. Buat sub-modul KMP baru `:printer-cloud` agar ukuran lib utama tetap ringan.
2. **Logika Agen Latar Belakang (Headless Printing)**:
   - AI harus menggunakan `WorkManager` untuk Android dan `BGTaskScheduler` untuk iOS. Selama aplikasi berada di latar belakang (Background), koneksi Bluetooth/TCP *PrinterConnector* tidak boleh dibunuh.
3. **Standar Payload Web-Socket / MQTT**:
   - Definisikan standard payload penerimaan JSON universal:
     ```json
     {
         "client_id": "kasir-cabang-jakarta-1",
         "action": "REMOTE_PRINT",
         "printer_mac": "00:11:22:33:44",
         "base64_escpos_payload": "GwRAAA..."
     }
     ```
   - Saat packet MQTT masuk, agen *Cloud Printer* KmpPrinter secara diam-diam (*silent*) akan men-decode string base64 menjadi `ByteArray` dan langsung dipompa ke `KmpPrinter.printRaw()`.

---
*End of Blueprint. AI, execute with extreme caution. This represents absolute industry mastery in KMP.*
