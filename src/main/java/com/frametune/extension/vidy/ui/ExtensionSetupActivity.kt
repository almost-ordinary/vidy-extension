package com.frametune.extension.vidy.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.frametune.extension.vidy.R
import com.frametune.extension.vidy.player.VidyPlayerActivity
import com.frametune.extension.vidy.service.LocalAddonService

class ExtensionSetupActivity : AppCompatActivity() {

    private lateinit var tvServerStatus: TextView
    private lateinit var tvStatusDot: TextView
    private lateinit var btnLinkFrameTune: Button
    private lateinit var btnTestPlayer: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_extension_setup)

        tvServerStatus = findViewById(R.id.tvServerStatus)
        tvStatusDot = findViewById(R.id.tvStatusDot)
        btnLinkFrameTune = findViewById(R.id.btnLinkFrameTune)
        btnTestPlayer = findViewById(R.id.btnTestPlayer)

        // Ensure background service is running
        LocalAddonService.start(this)

        updateStatusDisplay()

        // Setup visual focus scaling for TV remote D-pad
        setupFocusScaling(btnLinkFrameTune)
        setupFocusScaling(btnTestPlayer)

        btnLinkFrameTune.setOnClickListener {
            val manifestUrl = "http://127.0.0.1:8910/manifest.json"
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Vidy Addon", manifestUrl))
            Toast.makeText(
                this,
                "Copied '$manifestUrl'!\nOpen FrameTune TV > Extensions Sidebar to add.",
                Toast.LENGTH_LONG
            ).show()
        }

        btnTestPlayer.setOnClickListener {
            // Test launch Inception (TMDB ID 27205)
            val testIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vidy://play?type=movie&tmdb=27205")).apply {
                setClass(this@ExtensionSetupActivity, VidyPlayerActivity::class.java)
            }
            startActivity(testIntent)
        }

        btnLinkFrameTune.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        updateStatusDisplay()
    }

    private fun updateStatusDisplay() {
        val isRunning = LocalAddonService.isServerRunning()
        if (isRunning) {
            tvStatusDot.setTextColor(0xFF22C55E.toInt()) // Green
            tvServerStatus.text = "Local Loopback Server: http://127.0.0.1:8910 (Active)"
        } else {
            tvStatusDot.setTextColor(0xFFEAB308.toInt()) // Yellow
            tvServerStatus.text = "Starting Loopback Server on 127.0.0.1:8910..."
        }
    }

    private fun setupFocusScaling(view: View) {
        view.setOnFocusChangeListener { v, hasFocus ->
            val scale = if (hasFocus) 1.08f else 1.0f
            v.animate().scaleX(scale).scaleY(scale).setDuration(120).start()
        }
    }
}
