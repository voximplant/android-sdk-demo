/*
 * Copyright (c) 2017, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.push;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.voximplant.sdkdemo.SDKDemoApplication;

public class VoxFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        ((SDKDemoApplication) getApplication()).getClientManager().firebaseTokenRefreshed(FirebaseInstanceId.getInstance().getToken());
    }

}
