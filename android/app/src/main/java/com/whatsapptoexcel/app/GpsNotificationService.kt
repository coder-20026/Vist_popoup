package com.whatsapptoexcel.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import java.util.Locale

class GpsNotificationService : Service() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationCallback: LocationCallback? = null
    private var lastKnownLocationText = ""

    companion object {
        const val CHANNEL_ID = "gps_location_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.whatsapptoexcel.app.ACTION_START"
        const val ACTION_STOP = "com.whatsapptoexcel.app.ACTION_STOP"
        const val ACTION_COPY = "com.whatsapptoexcel.app.ACTION_COPY"
        const val ACTION_REFRESH = "com.whatsapptoexcel.app.ACTION_REFRESH"
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification("Dhoond rahe hain..."))
                requestLocationUpdates()
            }
            ACTION_REFRESH -> {
                requestSingleLocation()
            }
            ACTION_COPY -> {
                if (lastKnownLocationText.isNotEmpty() && lastKnownLocationText != "Dhoond rahe hain...") {
                    copyToClipboard(lastKnownLocationText)
                    Toast.makeText(this, "Location copy ho gayi hai: $lastKnownLocationText", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Location abhi available nahi hai", Toast.LENGTH_SHORT).show()
                }
                stopForegroundService()
            }
            ACTION_STOP -> {
                stopForegroundService()
            }
        }

        return START_STICKY
    }

    private fun stopForegroundService() {
        locationCallback?.let {
            fusedLocationClient.removeLocationUpdates(it)
        }
        stopForeground(true)
        stopSelf()
    }

    private fun requestLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 15000)
            .setMinUpdateIntervalMillis(10000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val loc = locationResult.lastLocation
                if (loc != null) {
                    updateLocation(loc)
                }
            }
        }

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun requestSingleLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    updateLocation(loc)
                }
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    private fun updateLocation(loc: Location) {
        lastKnownLocationText = String.format(Locale.US, "%.4f,%.4f", loc.latitude, loc.longitude)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(lastKnownLocationText))
    }

    private fun buildNotification(contentText: String): Notification {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        // Open app when notification clicked
        val openAppIntent = Intent(this, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, flag)

        // Copy button action
        val copyIntent = Intent(this, GpsNotificationService::class.java).apply {
            action = ACTION_COPY
        }
        val copyPendingIntent = PendingIntent.getService(this, 1, copyIntent, flag)

        // Refresh button action
        val refreshIntent = Intent(this, GpsNotificationService::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getService(this, 2, refreshIntent, flag)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Field Work GPS")
            .setContentText("Location: $contentText")
            .setOngoing(true)
            .setContentIntent(openAppPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(Notification.CATEGORY_SERVICE)

        if (contentText.isNotEmpty() && contentText != "Dhoond rahe hain...") {
            builder.addAction(android.R.drawable.ic_menu_save, "Copy", copyPendingIntent)
            builder.addAction(android.R.drawable.ic_menu_rotate, "Refresh", refreshPendingIntent)
        }

        return builder.build()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("GPS Coordinate", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "GPS Location Channel"
            val descriptionText = "Shows real-time location coordinate notification"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
