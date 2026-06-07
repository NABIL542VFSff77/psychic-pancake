package com.dynamicisland.spotify

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat

class FloatingMusicService : Service() {

    private lateinit var windowManager: WindowManager
    private var dynamicIslandView: DynamicIslandView? = null
    private var isViewShowing = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    dynamicIslandView?.hideInstantly()
                    isViewShowing = false
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (currentTrack != null) {
                        showIsland()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        registerScreenReceiver()
        instance = this
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> {
                val title = intent.getStringExtra(EXTRA_TITLE) ?: return START_STICKY
                val artist = intent.getStringExtra(EXTRA_ARTIST) ?: ""
                val art = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_ART, Bitmap::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_ART)
                }
                updateTrack(title, artist, art)
            }
            ACTION_HIDE -> {
                hideIsland()
                currentTrack = null
            }
        }
        return START_STICKY
    }

    private fun updateTrack(title: String, artist: String, art: Bitmap?) {
        currentTrack = TrackInfo(title, artist, art)
        if (!isViewShowing) {
            showIsland()
        } else {
            dynamicIslandView?.updateTrackInfo(title, artist, art)
        }
    }

    private fun showIsland() {
        if (!isViewShowing && currentTrack != null) {
            if (dynamicIslandView == null) {
                createView()
            }
            dynamicIslandView?.updateTrackInfo(
                currentTrack!!.title,
                currentTrack!!.artist,
                currentTrack!!.art
            )
            dynamicIslandView?.animateIn()
            isViewShowing = true
        }
    }

    private fun hideIsland() {
        dynamicIslandView?.animateOut {
            removeView()
            isViewShowing = false
        }
    }

    private fun createView() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val savedX = prefs.getInt(PREF_X, 0)
        val savedY = prefs.getInt(PREF_Y, DEFAULT_Y)
        val widthDp = prefs.getInt(PREF_WIDTH, DEFAULT_WIDTH_DP)
        val heightDp = prefs.getInt(PREF_HEIGHT, DEFAULT_HEIGHT_DP)

        val density = resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            widthPx,
            heightPx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.x = savedX
        params.y = savedY

        dynamicIslandView = DynamicIslandView(this, params, windowManager)
        windowManager.addView(dynamicIslandView, params)
    }

    private fun removeView() {
        dynamicIslandView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {}
        }
        dynamicIslandView = null
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(screenReceiver, filter)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Dynamic Island Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Dynamic Island overlay running"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Dynamic Island Active")
            .setContentText("Listening for Spotify playback")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        try { unregisterReceiver(screenReceiver) } catch (_: Exception) {}
        removeView()
    }

    data class TrackInfo(val title: String, val artist: String, val art: Bitmap?)

    companion object {
        var instance: FloatingMusicService? = null
        var currentTrack: TrackInfo? = null

        const val PREFS_NAME = "dynamic_island_prefs"
        const val PREF_X = "pos_x"
        const val PREF_Y = "pos_y"
        const val PREF_WIDTH = "width_dp"
        const val PREF_HEIGHT = "height_dp"
        const val DEFAULT_Y = 40
        const val DEFAULT_WIDTH_DP = 220
        const val DEFAULT_HEIGHT_DP = 36

        const val ACTION_SHOW = "com.dynamicisland.ACTION_SHOW"
        const val ACTION_HIDE = "com.dynamicisland.ACTION_HIDE"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ARTIST = "extra_artist"
        const val EXTRA_ART = "extra_art"

        private const val NOTIFICATION_ID = 8001
        private const val CHANNEL_ID = "dynamic_island_channel"
    }
}
