/*
 * Copyright (c) 2017, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import com.voximplant.sdk.messaging.IMessageEvent;
import com.voximplant.sdk.messaging.IMessengerEvent;
import com.voximplant.sdk.messaging.MessengerEventType;
import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.SDKDemoApplication;
import com.voximplant.sdkdemo.ui.calls.MainActivity;

public class NotificationHelper {
    private static NotificationHelper instance = null;
	private static NotificationManager mNotificationManager;
	private static int notificationId = 0;

    private NotificationHelper(Context context) {
        mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID,
                    "Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            channel.setDescription("description");
            channel.enableLights(true);
            channel.setLightColor(Color.BLUE);

            mNotificationManager.createNotificationChannel(channel);
        }
    }

    public static void init(Context context) {
        if (instance == null) {
            instance = new NotificationHelper(context);
        }
    }

    public static NotificationHelper get() {
        if (instance == null) {
            throw new IllegalStateException("NotificationHelper is not initialized");
        }
        return instance;
    }


    public Notification buildCallNotification(String text, Context context) {
        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0);
        return new NotificationCompat.Builder(context, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Voximplant")
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_vox_notification)
                .build();
    }

	public int buildMessengerNotification(IMessengerEvent event, Context context) {
		NotificationCompat.Builder builder = null;
		if (event.getMessengerEventType() == MessengerEventType.ON_SEND_MESSAGE) {
			IMessageEvent messageEvent = (IMessageEvent)event;
			builder = new NotificationCompat.Builder(context)
					.setSmallIcon(R.mipmap.ic_launcher_round)
					.setContentTitle("New message from " + messageEvent.getMessage().getSender())
					.setContentText(messageEvent.getMessage().getText())
					.setPriority(Notification.PRIORITY_HIGH)
					.setAutoCancel(true);
		}

		if (builder != null) {
			mNotificationManager.notify(notificationId, builder.build());
			notificationId++;
			return notificationId;
		} else {
			return -1;
		}
	}

	public static void cancelNotification(int notificationId) {
		if (notificationId > -1) {
			mNotificationManager.cancel(notificationId);
		}
	}

}