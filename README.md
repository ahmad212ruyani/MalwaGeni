# MalwaGeni - Accessible Android POS, Inventory & Dual-Ledger System

Aplikasi Android Native modern (Kotlin + Jetpack Compose) yang memadukan sistem **Kasir (POS)**, **Manajemen Stok Barang (Inventory)**, dan **Pencatatan Keuangan Pribadi (Dual-Ledger)**. Dirancang dari dasar dengan **prioritas aksesibilitas penuh (100% TalkBack / Screen Reader-Friendly)** dan otomatisasi CI/CD via **GitHub Actions**.

---

## ♿ Fitur & Standar Aksesibilitas (TalkBack)

1. **Touch Target Standar (Min 48dp x 48dp)**:
   - Seluruh elemen interaktif, tombol aksi, dan tab navigasi memiliki area klik minimal `48dp` untuk mempermudah navigasi motorik dan pengguna tunanetra.
2. **Pengumuman Suara & LiveRegion**:
   - Status koneksi (Online/Offline) dan peristiwa penting (penambahan keranjang, checkout sukses, restock barang) otomatis diumumkan via `AccessibilityEvent.TYPE_ANNOUNCEMENT` dan modifier `liveRegion = LiveRegionMode.Polite`.
3. **Linear & Merged Focus Hierarchy**:
   - Kartu produk dan transaksi menggunakan `Modifier.semantics(mergeDescendants = true)` sehingga TalkBack membacakan satu paket informasi produk/transaksi secara utuh dan terstruktur dalam satu kali usapan jari (*swipe*), bukan terfragmentasi.
4. **Kontras Tinggi & Skala Font Fleksibel**:
   - Menggunakan palet kontras tinggi (WCAG AAA >= 7:1) dan ukuran teks berskala (`sp`) yang responsif terhadap setelan aksesibilitas font sistem Android.

---

## 🏗️ Struktur Proyek

```text
MalwaGeni/
├── .github/
│   └── workflows/
│       └── build-apk.yml               # Pipeline CI/CD GitHub Actions
├── app/
│   ├── build.gradle.kts                # Konfigurasi dependensi Compose & SDK 34
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/malwageni/app/
│       │   ├── MainActivity.kt         # Entry point Activity
│       │   ├── accessibility/
│       │   │   └── AccessibilityUtils.kt # Helper TalkBack & LiveRegion
│       │   ├── model/
│       │   │   ├── ProductItem.kt      # Model katalog produk & barcode
│       │   │   └── TransactionItem.kt  # Model dual-ledger & penjualan POS
│       │   ├── network/
│       │   │   └── NetworkConnectivityObserver.kt # Pemantau koneksi realtime
│       │   └── ui/
│       │       ├── components/
│       │       │   └── AccessibleComponents.kt # Komponen UI min 48dp & accessible
│       │       ├── home/
│       │       │   ├── MainViewModel.kt    # State management & event audio
│       │       │   └── MainScreen.kt       # Antarmuka Jetpack Compose
│       │       └── theme/
│       │           ├── Color.kt        # Palet warna kontras tinggi
│       │           ├── Theme.kt        # Material 3 Theme
│       │           └── Type.kt         # Tipografi responsif
│       └── res/
│           ├── values/
│           │   ├── colors.xml
│           │   ├── strings.xml
│           │   └── themes.xml
│           └── mipmap-anydpi-v26/
├── build.gradle.kts                    # Root build script
├── settings.gradle.kts                 # Repositories & module includes
├── gradle.properties                   # Optimasi JVM & AndroidX
├── gradlew & gradlew.bat               # Wrapper script build
└── README.md
```

---

## 🚀 Panduan Git & Menjalankan Build di GitHub Actions

Untuk memulai repositori dan memicu build APK otomatis pertama kali:

### 1. Inisialisasi Git dan Buat Commit Pertama
Jalankan perintah berikut di terminal pada direktori `C:\MalwaGeni`:

```bash
git init
git add .
git commit -m "feat: initial accessible MalwaGeni app with CI/CD pipeline"
git branch -M main
```

### 2. Hubungkan ke GitHub Remote Repository
Buat repositori baru di GitHub (misal: `username/MalwaGeni`), lalu jalankan:

```bash
git remote add origin https://github.com/USERNAME/REPO_NAME.git
git push -u origin main
```

### 3. Pantau Build & Unduh File APK
1. Buka repositori Anda di GitHub.
2. Klik tab **Actions** di bagian atas menu.
3. Anda akan melihat workflow **Build Android APK (CI/CD)** sedang berjalan.
4. Setelah build selesai (tanda centang hijau), klik nama workflow tersebut.
5. Gulir ke bagian **Artifacts** di bagian bawah halaman untuk langsung mengunduh:
   - `MalwaGeni-Debug-APK`
   - `MalwaGeni-Release-APK`

---

## 🔑 Konfigurasi Signed Release APK (Opsional)

Jika Anda ingin file Release APK otomatis ditandatangani (*signed*) menggunakan Keystore produksi:
1. Konversikan file `.jks` ke format base64:
   ```bash
   base64 -w 0 my-release-key.jks > keystore_base64.txt
   ```
2. Buka **Settings** > **Secrets and variables** > **Actions** pada repositori GitHub Anda.
3. Tambahkan Secrets berikut:
   - `KEYSTORE_BASE64`: Isi teks base64 dari file keystore Anda.
   - `KEYSTORE_PASSWORD`: Password keystore.
   - `KEY_ALIAS`: Alias key.
   - `KEY_PASSWORD`: Password key.
4. Buka file `.github/workflows/build-apk.yml` dan aktifkan step `Decode Keystore from GitHub Secrets`.
