package com.whatsapptoexcel.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

class GpsStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            
            val hasLocationPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (isGpsEnabled && hasLocationPermission) {
                val serviceIntent = Intent(context, GpsNotificationService::class.java).apply {
                    action = GpsNotificationService.ACTION_START
                }
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    // Fail-safe
                }
            } else {
                val serviceIntent = Intent(context, GpsNotificationService::class.java).apply {
                    action = GpsNotificationService.ACTION_STOP
                }
                try {
                    context.stopService(serviceIntent)
                } catch (e: Exception) {
                    // Fail-safe
                }
            }
        }
    }
}
