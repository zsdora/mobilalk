package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotificationHandler {
    private static final String CHANNEL_ID = "shop_notification_channel";
    private final int NOTIFICATION_ID = 0;
    private  Context mContext;
    private NotificationManager mManager;
    public NotificationHandler(Context context) {
        this.mContext = context;
        this.mManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        createChannel();
    }

    private void createChannel(){
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return;


        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Bolt értesítés",
                NotificationManager.IMPORTANCE_HIGH);

        channel.enableLights(true);
        channel.enableVibration(true);
        channel.setLightColor(Color.RED);
        channel.setDescription("Értesítések a Repjegyek alkalmazásból.");
        this.mManager.createNotificationChannel(channel);
    }

    public void send(String message){
        Intent intent = new Intent(mContext, ShopListActivity.class);
         PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setContentTitle("Repjegyek applikáció")
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_shopping_cart)
                .setContentIntent(pendingIntent);
        this.mManager.notify(NOTIFICATION_ID, builder.build());
    }

    public void cancel(){
        this.mManager.cancel(NOTIFICATION_ID);
    }
}
