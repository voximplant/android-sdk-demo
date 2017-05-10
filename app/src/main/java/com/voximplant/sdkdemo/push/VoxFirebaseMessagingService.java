/*
 * Copyright (c) 2017, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.push;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.voximplant.sdkdemo.SDKDemoApplication;

public class VoxFirebaseMessagingService extends FirebaseMessagingService {

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        ((SDKDemoApplication) getApplication()).getClientManager().pushNotificationReceived(remoteMessage.getData());
    }
}
