#!/system/bin/sh
# UltraPower - bagian boot-time Disable Thermal (berjalan SEBELUM modul di-mount).
# THERMAL=off di /data/adb/ultrapower/config  ->  props thermal dimatikan + file thermal bawaan ditimpa file kosong (overlay).
# THERMAL=on / safe mode (file "skip") -> overlay dibuang, perilaku bawaan kembali setelah reboot.
MODDIR=${0%/*}
DIR=/data/adb/ultrapower
off=0
[ -f $DIR/config ] && grep -q '^THERMAL=off' $DIR/config && [ ! -f $DIR/skip ] && off=1

[ -f $MODDIR/thermal_overlay.list ] || exit 0

if [ $off = 1 ]; then
  while IFS= read -r f; do
    case "$f" in ''|/*|*..*) continue;; esac
    mkdir -p "$MODDIR/${f%/*}"
    [ -e "$MODDIR/$f" ] || : > "$MODDIR/$f"
    case "$f" in */bin/*) chmod 0755 "$MODDIR/$f";; *) chmod 0644 "$MODDIR/$f";; esac
  done < $MODDIR/thermal_overlay.list

  # props boot-time (dari system.prop + post-fs-data.sh modul Disable Thermal)
  for kv in \
    dalvik.vm.dexopt.thermal-cutoff=0 ro.vendor.mtk_thermal_2_0=0 ro.vendor.tran.hbm.thermal.temp.clr=0 \
    ro.vendor.tran.hbm.thermal.temp.trig=0 persist.vendor.powerhal.thermal_ux_temp_max=0 \
    persist.vendor.powerhal.thermal_ux_temp_min=0 ro.esports.thermal_config.support=0 ro.esports.thermal_config.version=0 \
    ro.boottime.mi_thermald=0 ro.boottime.thermal=0 ro.boottime.vendor.thermal-hal-2-0.mtk=0 \
    ro.boottime.vendor.thermal-mediatek=0 ro.boottime.thermal_manager=0 ro.boottime.thermald=0 \
    ro.boottime.thermalloadalgod=0 persist.thermal_config.mitigation=0 ro.mtk_thermal_monitor.enabled=false \
    ro.vendor.fps.switch.thermal=false ro.dar.thermal_core.support=0 ro.vendor.powerhal.thermal_ux_support=0 \
    ro.vendor.thermalspace.support=0 ro.boottime.thermal_space=0
  do
    resetprop -n "${kv%%=*}" "${kv#*=}"
  done
else
  while IFS= read -r f; do
    case "$f" in ''|/*|*..*) continue;; esac
    rm -f "$MODDIR/$f"
  done < $MODDIR/thermal_overlay.list
  [ -d $MODDIR/system/vendor ] && find $MODDIR/system/vendor -depth -type d -empty -exec rmdir {} \; 2>/dev/null
  [ -d $MODDIR/system/etc ] && find $MODDIR/system/etc -depth -type d -empty -exec rmdir {} \; 2>/dev/null
fi
exit 0
