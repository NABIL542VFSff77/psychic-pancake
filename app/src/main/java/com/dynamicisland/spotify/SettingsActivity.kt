package com.dynamicisland.spotify

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        supportActionBar?.title = "Dynamic Island Settings"

        prefs = getSharedPreferences(FloatingMusicService.PREFS_NAME, MODE_PRIVATE)

        val widthSeek = findViewById<SeekBar>(R.id.seekWidth)
        val heightSeek = findViewById<SeekBar>(R.id.seekHeight)
        val yPosSeek = findViewById<SeekBar>(R.id.seekYPos)
        val widthLabel = findViewById<TextView>(R.id.labelWidth)
        val heightLabel = findViewById<TextView>(R.id.labelHeight)
        val yPosLabel = findViewById<TextView>(R.id.labelYPos)
        val btnReset = findViewById<Button>(R.id.btnReset)
        val btnToggle = findViewById<Button>(R.id.btnToggle)

        val currentWidth = prefs.getInt(FloatingMusicService.PREF_WIDTH, FloatingMusicService.DEFAULT_WIDTH_DP)
        val currentHeight = prefs.getInt(FloatingMusicService.PREF_HEIGHT, FloatingMusicService.DEFAULT_HEIGHT_DP)
        val currentY = prefs.getInt(FloatingMusicService.PREF_Y, FloatingMusicService.DEFAULT_Y)

        widthSeek.max = 340
        widthSeek.min = 100
        widthSeek.progress = currentWidth
        widthLabel.text = "Width: ${currentWidth}dp"

        heightSeek.max = 56
        heightSeek.min = 28
        heightSeek.progress = currentHeight
        heightLabel.text = "Height: ${currentHeight}dp"

        yPosSeek.max = 200
        yPosSeek.min = 0
        yPosSeek.progress = currentY
        yPosLabel.text = "Top offset: ${currentY}px"

        widthSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                widthLabel.text = "Width: ${progress}dp"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                prefs.edit().putInt(FloatingMusicService.PREF_WIDTH, sb.progress).apply()
                FloatingMusicService.instance?.let { svc ->
                    val h = prefs.getInt(FloatingMusicService.PREF_HEIGHT, FloatingMusicService.DEFAULT_HEIGHT_DP)
                    svc.let { }
                }
                applySize()
            }
        })

        heightSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                heightLabel.text = "Height: ${progress}dp"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                prefs.edit().putInt(FloatingMusicService.PREF_HEIGHT, sb.progress).apply()
                applySize()
            }
        })

        yPosSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                yPosLabel.text = "Top offset: ${progress}px"
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {
                prefs.edit().putInt(FloatingMusicService.PREF_Y, sb.progress).apply()
            }
        })

        btnReset.setOnClickListener {
            prefs.edit()
                .putInt(FloatingMusicService.PREF_WIDTH, FloatingMusicService.DEFAULT_WIDTH_DP)
                .putInt(FloatingMusicService.PREF_HEIGHT, FloatingMusicService.DEFAULT_HEIGHT_DP)
                .putInt(FloatingMusicService.PREF_Y, FloatingMusicService.DEFAULT_Y)
                .putInt(FloatingMusicService.PREF_X, 0)
                .apply()
            widthSeek.progress = FloatingMusicService.DEFAULT_WIDTH_DP
            heightSeek.progress = FloatingMusicService.DEFAULT_HEIGHT_DP
            yPosSeek.progress = FloatingMusicService.DEFAULT_Y
            widthLabel.text = "Width: ${FloatingMusicService.DEFAULT_WIDTH_DP}dp"
            heightLabel.text = "Height: ${FloatingMusicService.DEFAULT_HEIGHT_DP}dp"
            yPosLabel.text = "Top offset: ${FloatingMusicService.DEFAULT_Y}px"
            applySize()
            Toast.makeText(this, "Reset to defaults", Toast.LENGTH_SHORT).show()
        }

        btnToggle.setOnClickListener {
            if (FloatingMusicService.instance == null) {
                val intent = Intent(this, FloatingMusicService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                btnToggle.text = "Service: Running"
                Toast.makeText(this, "Service started", Toast.LENGTH_SHORT).show()
            } else {
                stopService(Intent(this, FloatingMusicService::class.java))
                btnToggle.text = "Service: Stopped — tap to restart"
                Toast.makeText(this, "Service stopped", Toast.LENGTH_SHORT).show()
            }
        }

        btnToggle.text = if (FloatingMusicService.instance != null) "Service: Running" else "Service: Stopped — tap to start"
    }

    private fun applySize() {
        val w = prefs.getInt(FloatingMusicService.PREF_WIDTH, FloatingMusicService.DEFAULT_WIDTH_DP)
        val h = prefs.getInt(FloatingMusicService.PREF_HEIGHT, FloatingMusicService.DEFAULT_HEIGHT_DP)
        FloatingMusicService.instance?.let { svc ->
            val view = svc.javaClass.getDeclaredField("dynamicIslandView").apply { isAccessible = true }.get(svc) as? DynamicIslandView
            view?.resize(w, h)
        }
    }
}
