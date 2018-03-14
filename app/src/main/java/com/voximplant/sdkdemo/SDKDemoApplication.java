/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo;

import android.app.Application;

import com.voximplant.sdk.Voximplant;
import com.voximplant.sdk.client.ClientConfig;
import com.voximplant.sdk.client.IClient;
import com.voximplant.sdkdemo.manager.VoxCallManager;
import com.voximplant.sdkdemo.manager.VoxClientManager;
import com.voximplant.sdkdemo.utils.ForegroundCheck;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.util.concurrent.Executors;

public class SDKDemoApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ForegroundCheck.init(this);
        SharedPreferencesHelper.init(getApplicationContext());

        ClientConfig clientConfig = new ClientConfig();
//      clientConfig.enableDebugLogging = true;
        IClient client = Voximplant.getClientInstance(Executors.newSingleThreadExecutor(), getApplicationContext(), clientConfig);
        VoxClientManager clientManager = new VoxClientManager();
        clientManager.setClient(client);
        VoxCallManager callManager = new VoxCallManager(client, getApplicationContext());
        Shared.getInstance().setClientManager(clientManager);
        Shared.getInstance().setCallManager(callManager);
    }
}
