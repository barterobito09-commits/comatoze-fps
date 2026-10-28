SKIPUNZIP=0
ui_print "- Memasang UltraPower All-in-One"
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm $MODPATH/system/bin/ultrapower 0 0 0755
set_perm $MODPATH/service.sh 0 0 0755
set_perm $MODPATH/post-fs-data.sh 0 0 0755

# Disable Thermal (brutal): daftar file thermal bawaan perangkat yang akan ditimpa file kosong saat THERMAL=off (lihat post-fs-data.sh)
ui_print "- Memindai file konfigurasi thermal perangkat"
find /system/vendor/ -name '*thermal*.conf' -type f 2>/dev/null | while read -r nama; do
  echo "${nama#/}" >> $MODPATH/thermal_overlay.list
done
sort -u $MODPATH/thermal_overlay.list > $MODPATH/thermal_overlay.tmp && mv $MODPATH/thermal_overlay.tmp $MODPATH/thermal_overlay.list
ui_print "- Selesai. Buka WebUI modul atau jalankan 'su -c ultrapower status'"

if [ -f $MODPATH/apk/UltraFPS.apk ]; then
  ui_print "- Memasang aplikasi UltraFPS"
  cp $MODPATH/apk/UltraFPS.apk /data/local/tmp/ufps.apk && chmod 644 /data/local/tmp/ufps.apk
  if pm install -r /data/local/tmp/ufps.apk >/dev/null 2>&1; then
    appops set com.ultrapower.fps SYSTEM_ALERT_WINDOW allow
    pm grant com.ultrapower.fps android.permission.POST_NOTIFICATIONS 2>/dev/null
    ui_print "- UltraFPS terpasang"
  else
    ui_print "! Gagal memasang UltraFPS (dicoba lagi saat boot)"
  fi
  rm -f /data/local/tmp/ufps.apk
else
  ui_print "- APK UltraFPS tidak disertakan (opsional), lihat apk/README.txt"
fi
