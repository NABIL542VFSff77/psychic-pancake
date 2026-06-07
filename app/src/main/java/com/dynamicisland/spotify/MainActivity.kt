package com.dynamicisland.spotify

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        if (!hasOverlayPermission()) {
            requestOverlayPermission()
        } else if (!hasNotificationListenerPermission()) {
            requestNotificationListenerPermission()
        } else {
            startService()
            openSettings()
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun hasNotificationListenerPermission(): Boolean {
        val cn = ComponentName(this, SpotifyNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return !TextUtils.isEmpty(flat) && flat.contains(cn.flattenToString())
    }

    private fun requestOverlayPermission() {
        AlertDialog.Builder(this)
            .setTitle("Overlay Permission Required")
            .setMessage(
                "Dynamic Island needs permission to draw over other apps. " +
                "This allows it to show the music overlay on top of any app."
            )
            .setPositiveButton("Grant Permission") { _, _ ->
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_OVERLAY)
            }
            .setCancelable(false)
            .show()
    }

    private fun requestNotificationListenerPermission() {
        AlertDialog.Builder(this)
            .setTitle("Notification Access Required")
            .setMessage(
                "Dynamic Island needs notification access to detect when Spotify is playing. " +
                "Please enable it in the next screen."
            )
            .setPositiveButton("Grant Permission") { _, _ ->
                startActivityForResult(
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                    REQUEST_NOTIFICATION
                )
            }
            .setCancelable(false)
            .show()
    }

    private fun startService() {
        val serviceIntent = Intent(this, FloatingMusicService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun openSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_OVERLAY -> {
                if (hasOverlayPermission()) {
                    if (!hasNotificationListenerPermission()) {
                        requestNotificationListenerPermission()
                    } else {
                        startService()
                        openSettings()
                    }
                } else {
                    requestOverlayPermission()
                }
            }
            REQUEST_NOTIFICATION -> {
                if (hasNotificationListenerPermission()) {
                    startService()
                    openSettings()
                } else {
                    requestNotificationListenerPermission()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_OVERLAY = 1001
        private const val REQUEST_NOTIFICATION = 1002
    }
}
