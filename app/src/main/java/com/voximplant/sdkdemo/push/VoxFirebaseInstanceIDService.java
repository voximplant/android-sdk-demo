/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.push;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.voximplant.sdkdemo.Shared;

public class VoxFirebaseInstanceIDService extends FirebaseInstanceIdService {

    @Override
    public void onTokenRefresh() {
        Shared.getInstance().getClientManager().firebaseTokenRefreshed(FirebaseInstanceId.getInstance().getToken());
    }

}
