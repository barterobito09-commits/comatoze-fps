package com.ultrapower.fps

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/** Tanpa tampilan: menyalakan FpsService lalu menutup diri. Dipakai module sebagai cadangan. */
class AutoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            startForegroundService(Intent(this, FpsService::class.java))
        } catch (_: Exception) {
        }
        finish()
    }
}
