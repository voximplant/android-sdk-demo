/*
 * Copyright (c) 2011-2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

import com.voximplant.sdk.messaging.IMessageEvent;
import com.voximplant.sdk.messaging.IMessengerEvent;
import com.voximplant.sdk.messaging.MessengerEventType;
import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.SDKDemoApplication;


public class NotificationHelper {

	private static NotificationHelper instance = null;
	private static NotificationManager mNotificationManager;
	private static int notificationId = 0;

	private NotificationHelper(Context context) {
		mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public static NotificationHelper init(Context context) {
		if (instance == null) {
			instance = new NotificationHelper(context);
		}
		return instance;
	}

	public static NotificationHelper get() {
		if (instance == null) {
			throw new IllegalStateException("NotificationHelper is not initialized");
		}
		return instance;
	}

	public int buildMessengerNotification(IMessengerEvent event) {
		NotificationCompat.Builder builder = null;
		if (event.getMessengerEventType() == MessengerEventType.ON_SEND_MESSAGE) {
			IMessageEvent messageEvent = (IMessageEvent)event;
			builder = new NotificationCompat.Builder(SDKDemoApplication.getAppContext())
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
