package com.ultrapower.fps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import java.io.File

/**
 * Overlay FPS. Tidak butuh root sendiri: angka FPS ditulis oleh daemon module UltraPower
 * ke file <filesDir>/fps dengan format "<fps>|<paket>", app ini hanya membacanya
 * dan menampilkannya.
 */
class FpsService : Service() {

    companion object {
        const val ACTION_STOP = "com.ultrapower.fps.STOP"
        const val PREFS = "ufps"
        private const val CHANNEL = "ufps"

        @Volatile
        var running = false
    }

    private lateinit var wm: WindowManager
    private lateinit var lp: WindowManager.LayoutParams
    private lateinit var prefs: SharedPreferences
    private lateinit var file: File
    private var view: FpsView? = null
    private var lastMod = 0L
    private val ui = Handler(Looper.getMainLooper())
    private val hidden = HashSet<String>()

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        applyPrefs()
    }
    private val tick = object : Runnable {
        override fun run() {
            poll()
            ui.postDelayed(this, 500)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        file = File(filesDir, "fps")
        hidden.add(packageName)
        hidden.add("com.android.systemui")
        homePkg()?.let { hidden.add(it) }
        running = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        goForeground()
        if (view == null) {
            if (!addOverlay()) {
                stopSelf()
                return START_NOT_STICKY
            }
            prefs.registerOnSharedPreferenceChangeListener(prefListener)
            ui.post(tick)
        }
        return START_STICKY
    }

    private fun homePkg(): String? = try {
        packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName
    } catch (_: Exception) {
        null
    }

    private fun goForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "UltraFPS", NotificationManager.IMPORTANCE_MIN)
        )
        val stop = PendingIntent.getService(
            this, 0,
            Intent(this, FpsService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("UltraFPS aktif")
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(this, android.R.drawable.ic_menu_close_clear_cancel),
                    "Stop",
                    stop
                ).build()
            )
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, n)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun addOverlay(): Boolean {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val v = FpsView(this)

        lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = prefs.getInt("x", dp(12))
            y = prefs.getInt("y", dp(48))
            if (Build.VERSION.SDK_INT >= 28) {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // Geser dengan jari, posisi disimpan
        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = lp.x
                    startY = lp.y
                    touchX = e.rawX
                    touchY = e.rawY
                }
                MotionEvent.ACTION_MOVE -> {
                    lp.x = startX + (e.rawX - touchX).toInt()
                    lp.y = startY + (e.rawY - touchY).toInt()
                    try {
                        wm.updateViewLayout(v, lp)
                    } catch (_: Exception) {
                    }
                }
                MotionEvent.ACTION_UP ->
                    prefs.edit().putInt("x", lp.x).putInt("y", lp.y).apply()
            }
            true
        }

        return try {
            wm.addView(v, lp)
            view = v
            applyPrefs()
            true
        } catch (e: Exception) {
            false // izin overlay belum diberikan
        }
    }

    private fun applyPrefs() {
        view?.configure(prefs.getInt("scale", 100), prefs.getBoolean("graph", true))
    }

    private fun poll() {
        val v = view ?: return
        try {
            if (!file.exists()) file.createNewFile()
        } catch (_: Exception) {
        }
        val m = file.lastModified()
        var fps = -1
        var pkg = ""
        if (m > 0 && System.currentTimeMillis() - m < 4000) {
            val p = try {
                file.readText().trim().split("|")
            } catch (_: Exception) {
                emptyList<String>()
            }
            fps = p.getOrNull(0)?.toIntOrNull() ?: -1
            pkg = p.getOrNull(1) ?: ""
        }
        val fresh = m != lastMod
        lastMod = m
        v.visibility = if (pkg in hidden) View.GONE else View.VISIBLE
        v.setFps(fps, fresh && fps >= 0)
    }

    override fun onDestroy() {
        running = false
        ui.removeCallbacks(tick)
        try {
            prefs.unregisterOnSharedPreferenceChangeListener(prefListener)
        } catch (_: Exception) {
        }
        view?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        view = null
        // Hapus file supaya daemon module berhenti menghitung FPS
        try {
            file.delete()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }
}
