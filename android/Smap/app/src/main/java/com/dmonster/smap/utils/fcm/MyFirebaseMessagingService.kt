package com.dmonster.smap.utils.fcm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.dmonster.smap.R
import com.dmonster.smap.view.main.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService: FirebaseMessagingService() {
    private val TAG = "MyFirebaseMessagingService"

    lateinit var title: String
    lateinit var body: String
    lateinit var event_url: String
    lateinit var image: String

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FirebaseInstanceIDService : $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val map: Map<String, String> = message.data

        Log.d(TAG, "" + map)
        for (key in map.keys) {
            Log.d(TAG, key + "(" + map[key] + ")")
        }

        title = map["title"].toString()
        body = map["body"].toString()
        event_url = map["event_url"].toString()
        image = map["image"].toString()

        sendNotification()
    }

    private fun sendNotification(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        var pendingIntentFlag = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            pendingIntentFlag = PendingIntent.FLAG_MUTABLE
        }

        val notificationId = (System.currentTimeMillis()).toInt()

        val pendingIntent = PendingIntent.getActivity(this, notificationId, makeIntent(), PendingIntent.FLAG_UPDATE_CURRENT or pendingIntentFlag)

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(this@MyFirebaseMessagingService)
            NotificationCompat.Builder(this, packageName + "-" + getString(R.string.app_name))
        } else {
            NotificationCompat.Builder(this)
        }

        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setContentTitle(title)
        builder.setContentText(body)
        builder.priority = NotificationCompat.PRIORITY_HIGH
        builder.setAutoCancel(true)
        builder.setContentIntent(pendingIntent)

        val bigViews = RemoteViews(packageName, R.layout.expanded_notification)
        bigViews.setTextViewText(R.id.noti_title, title)
        bigViews.setTextViewText(R.id.noti_body, body)

        Glide.with(applicationContext)
            .asBitmap()
            .load(image)
            .into(object : CustomTarget<Bitmap>() {

                @SuppressLint("MissingPermission")
                override fun onLoadFailed(errorDrawable: Drawable?) {
                    super.onLoadFailed(errorDrawable)
                    NotificationManagerCompat.from(this@MyFirebaseMessagingService).notify(notificationId, builder.build())
                }

                @SuppressLint("MissingPermission")
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: Transition<in Bitmap>?,
                ) {
                    builder.setLargeIcon(resource)
                    bigViews.setImageViewBitmap(R.id.noti_image, resource)

                    builder.setCustomBigContentView(bigViews)

                    NotificationManagerCompat.from(this@MyFirebaseMessagingService).notify(notificationId, builder.build())
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(context: Context) {
        val channelId = context.packageName + "-" + getString(R.string.app_name)
        val channel = NotificationChannel(channelId, getString(R.string.app_name), NotificationManager.IMPORTANCE_HIGH)
        channel.description = "App notification channel"
        channel.setShowBadge(true)
        channel.enableVibration(true)
        val notificationManager = context.getSystemService(
            NotificationManager::class.java
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun makeIntent(): Intent {
        val intent = Intent(applicationContext, MainActivity::class.java)
        intent.putExtra("event_url", event_url)
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return intent
    }
}