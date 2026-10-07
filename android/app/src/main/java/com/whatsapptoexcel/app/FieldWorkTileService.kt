package com.whatsapptoexcel.app

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

/**
 * Android Quick Settings TileService for "Field Work".
 * Allows field agents to toggle the floating overlay popup directly
 * from the Android Notification Shade / Quick Settings Panel.
 */
class FieldWorkTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()

        // 1. Check if overlay permission (Display over other apps) is granted
        if (!OverlayPermissionHelper.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "Field Work: 'Display over other apps' permission is required for floating popup",
                Toast.LENGTH_LONG
            ).show()

            val settingsIntent = OverlayPermissionHelper.createOverlaySettingsIntent(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    settingsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(settingsIntent)
            }
            return
        }

        // 2. Open or reuse existing floating popup (prevent duplicate popup)
        FieldFloatingService.startService(this)
        updateTileState(isActive = true)
    }

    private fun updateTileState(isActive: Boolean = FieldFloatingService.isRunning) {
        val tile = qsTile ?: return
        tile.label = getString(R.string.quick_tile_label)
        tile.contentDescription = getString(R.string.quick_tile_desc)
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }

    companion object {
        /**
         * Notifies the system to update the tile's visual state (Active / Inactive).
         */
        fun requestListeningState(context: Context) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    TileService.requestListeningState(
                        context,
                        ComponentName(context, FieldWorkTileService::class.java)
                    )
                }
            } catch (e: Exception) {
                // Ignore if not supported on this device/ROM
            }
        }
    }
}
