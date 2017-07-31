/*
 * Copyright (c) 2017, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.push;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voximplant.sdk.Voximplant;
import com.voximplant.sdk.messaging.IMessengerEvent;
import com.voximplant.sdkdemo.SDKDemoApplication;
import com.voximplant.sdkdemo.utils.NotificationHelper;

import java.util.Map;

public class VoxFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> push = remoteMessage.getData();

        if (push.containsKey("voximplant")) {
            ((SDKDemoApplication) getApplication()).getClientManager().pushNotificationReceived(remoteMessage.getData());
        } else if (push.containsKey("voximplant_im")){
            IMessengerEvent event = Voximplant.getMessengerPushNotificationProcessing().processPushNotification(push);
            if (event != null) {
                NotificationHelper.get().buildMessengerNotification(event);
            }
        }
    }
}
