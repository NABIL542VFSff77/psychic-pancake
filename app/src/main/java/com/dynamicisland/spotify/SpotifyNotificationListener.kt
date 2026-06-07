package com.dynamicisland.spotify

import android.content.ComponentName
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SpotifyNotificationListener : NotificationListenerService() {

    private val handler = Handler(Looper.getMainLooper())
    private var lastTitle = ""
    private var isShowing = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            checkMediaSessions()
            handler.postDelayed(this, POLL_INTERVAL_MS)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        handler.post(pollRunnable)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        handler.removeCallbacks(pollRunnable)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (isSpotifyPackage(sbn.packageName)) {
            checkMediaSessions()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (isSpotifyPackage(sbn.packageName)) {
            checkMediaSessions()
        }
    }

    private fun checkMediaSessions() {
        try {
            val msm = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val cn = ComponentName(this, SpotifyNotificationListener::class.java)
            val sessions = msm.getActiveSessions(cn)

            for (session in sessions) {
                if (!isSpotifyPackage(session.packageName)) continue

                val state = session.playbackState?.state
                val isPlaying = state == PlaybackState.STATE_PLAYING
                    || state == PlaybackState.STATE_BUFFERING

                if (!isPlaying) {
                    if (isShowing) {
                        isShowing = false
                        sendHide()
                    }
                    return
                }

                val metadata = session.metadata ?: continue
                val title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
                    ?: metadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
                    ?: continue

                val artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: metadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
                    ?: ""

                val art: Bitmap? = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?: metadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)

                if (title != lastTitle || !isShowing) {
                    lastTitle = title
                    isShowing = true
                    sendShow(title, artist, art)
                }
                return
            }

            // No active Spotify session found
            if (isShowing) {
                isShowing = false
                lastTitle = ""
                sendHide()
            }
        } catch (e: SecurityException) {
            // Notification access not granted yet
        } catch (e: Exception) {
            // Ignore other errors
        }
    }

    private fun sendShow(title: String, artist: String, art: Bitmap?) {
        val intent = Intent(this, FloatingMusicService::class.java).apply {
            action = FloatingMusicService.ACTION_SHOW
            putExtra(FloatingMusicService.EXTRA_TITLE, title)
            putExtra(FloatingMusicService.EXTRA_ARTIST, artist)
            if (art != null) putExtra(FloatingMusicService.EXTRA_ART, art)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun sendHide() {
        val intent = Intent(this, FloatingMusicService::class.java).apply {
            action = FloatingMusicService.ACTION_HIDE
        }
        startService(intent)
    }

    private fun isSpotifyPackage(packageName: String): Boolean {
        return SPOTIFY_PACKAGES.any { packageName.startsWith(it) }
    }

    companion object {
        private val SPOTIFY_PACKAGES = listOf(
            "com.spotify.music",
            "com.spotify.lite",
            "com.spotify"
        )
        private const val POLL_INTERVAL_MS = 2000L
    }
}
