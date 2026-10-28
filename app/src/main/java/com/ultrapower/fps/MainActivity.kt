package com.ultrapower.fps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import java.io.File

private val BG = Color.parseColor("#0B0F14")
private val CARD = Color.parseColor("#121821")
private val GREEN = Color.parseColor("#00E58B")
private val MUTED = Color.parseColor("#8A94A6")
private val BTN = Color.parseColor("#1B222C")

class MainActivity : Activity() {

    private lateinit var prefs: android.content.SharedPreferences
    private lateinit var permLine: TextView
    private lateinit var linkLine: TextView
    private lateinit var permBtn: Button
    private lateinit var toggleBtn: Button
    private val ui = Handler(Looper.getMainLooper())
    private val loop = object : Runnable {
        override fun run() {
            refresh()
            ui.postDelayed(this, 1000)
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun round(color: Int, r: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(r).toFloat()
    }

    private fun text(s: String, size: Float, color: Int, bold: Boolean = false) =
        TextView(this).apply {
            text = s
            textSize = size
            setTextColor(color)
            if (bold) typeface = Typeface.DEFAULT_BOLD
        }

    private fun button(label: String, primary: Boolean, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        textSize = 15f
        setTextColor(if (primary) Color.parseColor("#04140D") else Color.WHITE)
        background = round(if (primary) GREEN else BTN, 14)
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(52)
        ).apply { topMargin = dp(10) }
        setOnClickListener { onClick() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences(FpsService.PREFS, MODE_PRIVATE)

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(32), dp(22), dp(24))
        }
        root.addView(text("UltraFPS", 30f, Color.WHITE, bold = true))
        root.addView(text("Overlay FPS untuk game", 14f, MUTED))

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = round(CARD, 16)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
        }
        permLine = text("", 14f, Color.WHITE)
        linkLine = text("", 14f, Color.WHITE).apply { setPadding(0, dp(6), 0, 0) }
        card.addView(permLine)
        card.addView(linkLine)
        root.addView(card)

        permBtn = button("Izinkan tampil di atas app lain", true) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
        toggleBtn = button("Nyalakan overlay", false) {
            if (!Settings.canDrawOverlays(this)) {
                refresh()
            } else {
                val i = Intent(this, FpsService::class.java)
                if (FpsService.running) stopService(i) else startForegroundService(i)
            }
        }
        root.addView(permBtn)
        root.addView(toggleBtn)

        val set = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = round(CARD, 16)
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(20) }
        }
        val graphSw = Switch(this).apply {
            text = "Grafik mini"
            setTextColor(Color.WHITE)
            isChecked = prefs.getBoolean("graph", true)
            setOnCheckedChangeListener { _, c -> prefs.edit().putBoolean("graph", c).apply() }
        }
        val sizeLabel = text("Ukuran: ${prefs.getInt("scale", 100)}%", 14f, Color.WHITE)
            .apply { setPadding(0, dp(14), 0, 0) }
        val sizeBar = SeekBar(this).apply {
            max = 80 // 70% .. 150%
            progress = prefs.getInt("scale", 100) - 70
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(s: SeekBar?, p: Int, fromUser: Boolean) {
                    val v = 70 + p
                    sizeLabel.text = "Ukuran: $v%"
                    if (fromUser) prefs.edit().putInt("scale", v).apply()
                }

                override fun onStartTrackingTouch(s: SeekBar?) {}
                override fun onStopTrackingTouch(s: SeekBar?) {}
            })
        }
        set.addView(graphSw)
        set.addView(sizeLabel)
        set.addView(sizeBar)
        root.addView(set)

        root.addView(
            text(
                "Geser angka FPS di layar untuk memindahkan posisinya. " +
                    "Tombol On/Off utama ada di WebUI UltraPower.",
                12f, MUTED
            ).apply { setPadding(0, dp(16), 0, 0) }
        )

        setContentView(ScrollView(this).apply {
            setBackgroundColor(BG)
            addView(root)
        })
    }

    override fun onResume() {
        super.onResume()
        ui.post(loop)
    }

    override fun onPause() {
        ui.removeCallbacks(loop)
        super.onPause()
    }

    private fun refresh() {
        val can = Settings.canDrawOverlays(this)
        permLine.text = if (can) "Izin overlay: aktif" else "Izin overlay: belum diberikan"
        permBtn.visibility = if (can) View.GONE else View.VISIBLE
        toggleBtn.text = if (FpsService.running) "Matikan overlay" else "Nyalakan overlay"

        val f = File(filesDir, "fps")
        val linked = FpsService.running && f.exists() && f.length() > 0 &&
            System.currentTimeMillis() - f.lastModified() < 4000
        linkLine.text = if (linked) "Data FPS: terhubung ke UltraPower"
        else "Data FPS: menunggu UltraPower (aktifkan FPS Overlay di WebUI)"
    }
}
