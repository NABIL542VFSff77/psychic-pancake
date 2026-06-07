package com.dynamicisland.spotify

import android.app.Notification
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class SpotifyNotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isSpotifyPackage(sbn.packageName)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val artist = extras.getString(Notification.EXTRA_TEXT) ?: ""

        val art: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG, Bitmap::class.java)
                ?: extras.getParcelable(Notification.EXTRA_LARGE_ICON, Bitmap::class.java)
        } else {
            @Suppress("DEPRECATION")
            extras.getParcelable(Notification.EXTRA_LARGE_ICON_BIG)
                ?: extras.getParcelable(Notification.EXTRA_LARGE_ICON)
        }

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

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (!isSpotifyPackage(sbn.packageName)) return

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
            "com.spotify.lite"
        )
    }
}
