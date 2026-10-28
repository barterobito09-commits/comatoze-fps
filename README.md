# UltraFPS

App overlay FPS untuk module **UltraPower**. Tampilan: pill gelap transparan dengan bar aksen,
angka FPS tebal, dan grafik mini 15 detik. Warna mengikuti FPS (hijau >= 55, kuning >= 30, merah di bawahnya).
Overlay otomatis hilang di home screen. Bisa digeser, ukuran dan grafik bisa diatur di app.

App ini tidak membaca FPS sendiri. Daemon root milik UltraPower menulis `<fps>|<paket>` ke
`/data/data/com.ultrapower.fps/files/fps`, app hanya menampilkannya. On/Off dari WebUI UltraPower
(fitur **FPS Overlay (aplikasi)**).

## Build lewat GitHub (tanpa laptop)

1. Push semua isi folder ini ke repo GitHub baru (termasuk folder `.github` dan `module`).
2. Tab **Actions** -> run terbaru -> tunggu hijau.
3. Unduh artifact:
   - `UltraPower-Module` -> zip module lengkap, sudah berisi `apk/UltraFPS.apk`
   - `UltraFPS-APK` -> APK saja

Push dari HP (Termux):
```
pkg install git unzip
unzip UltraFPS-Project.zip && cd UltraFPS
git init && git add . && git commit -m "init"
git branch -M main
git remote add origin https://github.com/USERNAME/REPO.git
git push -u origin main
```
Password = Personal Access Token GitHub.

## Pasang

Cara A (flash ulang module): flash `UltraPower-v2.2-FPS.zip` di Magisk/KernelSU, reboot.
`customize.sh` otomatis memasang UltraFPS dan memberi izin overlay.

Cara B (tanpa flash ulang): salin `UltraFPS.apk` ke `/sdcard/UltraPower/UltraFPS.apk`,
buka WebUI UltraPower -> tab Game -> nyalakan **FPS Overlay (aplikasi)**
(atau tombol **Pasang / Pasang Ulang UltraFPS**).

## Catatan

Angka FPS dibaca dari `dumpsys SurfaceFlinger --latency` oleh module. Kalau selalu `--` atau `0`,
cek dulu di terminal: `su -c "ultrapower fps"` saat game terbuka.
