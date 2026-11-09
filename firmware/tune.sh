#!/system/bin/sh
# Projector HY300-T tuning profile — apply on boot via OpenBot/systemd.
# Safe/idempotent: every write is guarded so a missing node never breaks boot.

log() { echo "tune: $*"; }

# ---- CPU: schedutil (modern EAS governor) + higher floor for snappier ramps
for c in /sys/devices/system/cpu/cpu[0-9]*; do
  [ -w "$c/cpufreq/scaling_governor" ] && echo schedutil > "$c/cpufreq/scaling_governor"
  [ -w "$c/cpufreq/scaling_min_freq" ] && echo 1000000 > "$c/cpufreq/scaling_min_freq"
  [ -w "$c/cpufreq/scaling_max_freq" ] && echo 1464000 > "$c/cpufreq/scaling_max_freq"
done
log "cpu governor=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor 2>/dev/null) min=$(cat /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq 2>/dev/null)"

# ---- GPU: keep the Mali/MPGPU off its lowest level
[ -w /sys/class/mpgpu/min_freq ] && echo 2 > /sys/class/mpgpu/min_freq
[ -w /sys/class/mpgpu/scale_mode ] && echo performance > /sys/class/mpgpu/scale_mode 2>/dev/null
log "gpu min=$(cat /sys/class/mpgpu/min_freq 2>/dev/null) mode=$(cat /sys/class/mpgpu/scale_mode 2>/dev/null)"

# ---- I/O: flash-friendly scheduler, bigger readahead for video
for q in /sys/block/mmcblk0/queue /sys/block/mmcblk1/queue; do
  [ -w "$q/scheduler" ] && echo none > "$q/scheduler"
  [ -w "$q/read_ahead_kb" ] && echo 512 > "$q/read_ahead_kb"
  [ -w "$q/nr_requests" ] && echo 256 > "$q/nr_requests"
done
log "io=$(cat /sys/block/mmcblk0/queue/scheduler 2>/dev/null) ra=$(cat /sys/block/mmcblk0/queue/read_ahead_kb 2>/dev/null)"

# ---- VM: less dirty jank, keep dentries hot, zram-friendly swappiness
echo 10 > /proc/sys/vm/dirty_ratio
echo 3 > /proc/sys/vm/dirty_background_ratio
echo 200 > /proc/sys/vm/dirty_expire_centisecs
echo 500 > /proc/sys/vm/dirty_writeback_centisecs
echo 100 > /proc/sys/vm/swappiness
echo 80 > /proc/sys/vm/vfs_cache_pressure
echo 16384 > /proc/sys/vm/min_free_kbytes
log "vm dirty=$(cat /proc/sys/vm/dirty_ratio) swap=$(cat /proc/sys/vm/swappiness)"

# ---- zram: grow to ~1.2G with lz4 (fast on armv7) so 1GB RAM stops thrashing
# Only safe when nothing is swapped out (i.e. right after boot).
USED_SWAP=$(awk 'NR==2 {print $4}' /proc/swaps)
if [ "$(cat /sys/block/zram0/disksize 2>/dev/null)" != "1288490188" ] && [ "${USED_SWAP:-0}" = "0" ]; then
  swapoff /dev/zram0 2>/dev/null
  echo 1 > /sys/block/zram0/reset 2>/dev/null
  echo lz4 > /sys/block/zram0/comp_algorithm 2>/dev/null
  echo 1288490188 > /sys/block/zram0/disksize 2>/dev/null
  mkswap /dev/zram0 >/dev/null 2>&1
  swapon -p 100 /dev/zram0 2>/dev/null || swapon /dev/zram0 2>/dev/null
  log "zram size=$(cat /sys/block/zram0/disksize 2>/dev/null)"
fi

# ---- quieter kernel logging (less printk overhead, better latency)
echo "4 4 1 7" > /proc/sys/kernel/printk
log "done"
