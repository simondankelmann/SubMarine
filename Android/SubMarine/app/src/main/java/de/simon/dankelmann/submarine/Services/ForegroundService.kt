package de.simon.dankelmann.submarine.Services

import android.app.*
import android.content.Context
import android.content.Intent;
import android.graphics.Color
import android.os.Binder
import android.os.Build
import android.os.IBinder;
import android.os.Message
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import de.simon.dankelmann.submarine.AppContext.AppContext
import de.simon.dankelmann.submarine.MainActivity

class ForegroundService : Service() {

    private val TAG_FOREGROUND_SERVICE = "FOREGROUND_SERVICE"
    val ACTION_START_FOREGROUND_SERVICE:String = "ACTION_START_FOREGROUND_SERVICE"
    val ACTION_STOP_FOREGROUND_SERVICE:String = "ACTION_STOP_FOREGROUND_SERVICE"


    private val myBinder = MyLocalBinder()

    lateinit var mNotification:Notification

    override fun onBind(intent: Intent): IBinder? {
        return myBinder
    }

    fun updateNotification(title:String, message: String){
        if(mNotification != null){

            val notifyIntent = Intent(AppContext.getContext(), MainActivity::class.java)
            val title = "Submarine"
            val message = "Submarine is watching your Location"

            notifyIntent.putExtra("title", title)
            notifyIntent.putExtra("message", message)
            notifyIntent.putExtra("notification", true)

            notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            val pendingIntent = PendingIntent.getActivity(AppContext.getContext(), 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            mNotification.contentIntent = pendingIntent

        }
    }

    fun getCurrentTime(): String {
        val dateformat = SimpleDateFormat("HH:mm:ss MM/dd/yyyy",
            Locale.US)
        return dateformat.format(Date())
    }

    inner class MyLocalBinder : Binder() {
        fun getService() : ForegroundService {
            return this@ForegroundService
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Send a notification that service is started
        //toast("Service started.")

        Log.d("_FG", "Entering onStartCommand")

        intent.let {
            var action = intent?.action

            if(action == ACTION_STOP_FOREGROUND_SERVICE){
                stopForegroundService()
            }

            if(action == ACTION_START_FOREGROUND_SERVICE){
                //Start Foreground
                getNotification(applicationContext)
            }
        }

        return START_STICKY
    }

    fun stopForegroundService() {
        Log.d(TAG_FOREGROUND_SERVICE, "Stop foreground service.")

        // Stop foreground service and remove the notification.
        stopForeground(true)

        // Stop the foreground service.
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        //toast("Service destroyed.")
        stopForeground(true)
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Stop foreground service and remove the notification.
        stopForeground(true)

        stopSelf()
    }

    companion object {
        const val CHANNEL_ID = "de.simon.dankelmann.submarine.location.service.channel.id"
        const val CHANNEL_NAME = "Submarine Location Service Channel"
    }

    private fun createChannel(context: Context) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val importance = NotificationManager.IMPORTANCE_HIGH
            val notificationChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance)
            notificationChannel.enableVibration(false)
            notificationChannel.setShowBadge(true)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.parseColor("#e8334a")
            notificationChannel.description = "Submarine Notifications"
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            notificationManager.createNotificationChannel(notificationChannel)
        }

    }

    private fun getNotification(context: Context) {
        //Create Channel
        createChannel(context)

        //var notificationManager: NotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifyIntent = Intent(context, MainActivity::class.java)
        val title = "Submarine"
        val message = "Submarine is watching your Location"

        notifyIntent.putExtra("title", title)
        notifyIntent.putExtra("message", message)
        notifyIntent.putExtra("notification", true)

        notifyIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK


        val pendingIntent = PendingIntent.getActivity(context, 0, notifyIntent, PendingIntent.FLAG_IMMUTABLE)

        //var mNotification = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            mNotification = Notification.Builder(context, CHANNEL_ID)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setAutoCancel(true)
                .setContentTitle(title)
                .setStyle(Notification.BigTextStyle()
                    .bigText(message))
                .setContentText(message).build()
        } else {

            mNotification = Notification.Builder(context)
                // Set the intent that will fire when the user taps the notification
                .setContentIntent(pendingIntent)
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setAutoCancel(true)
                .setPriority(Notification.PRIORITY_MAX)
                .setContentTitle(title)
                .setStyle(Notification.BigTextStyle()
                    .bigText(message))
                .setContentText(message).build()
        }

        startForeground(999, mNotification)
    }

}