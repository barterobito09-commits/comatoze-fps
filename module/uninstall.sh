#!/system/bin/sh
rm -rf /data/adb/ultrapower
for f in chg game wd; do [ -f /data/adb/ultrapower/$f.pid ] && kill $(cat /data/adb/ultrapower/$f.pid) 2>/dev/null; done
[ -f /data/adb/ultrapower/fps.pid ] && kill $(cat /data/adb/ultrapower/fps.pid) 2>/dev/null
pm uninstall com.ultrapower.fps >/dev/null 2>&1
