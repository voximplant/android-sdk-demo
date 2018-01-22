/*
 * Copyright (c) 2011-2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo;

import android.app.Application;
import android.content.Context;

import com.voximplant.sdk.client.ClientConfig;
import com.voximplant.sdk.client.IClient;
import com.voximplant.sdk.Voximplant;
import com.voximplant.sdkdemo.manager.VoxCallManager;
import com.voximplant.sdkdemo.manager.VoxClientManager;
import com.voximplant.sdkdemo.utils.ForegroundCheck;
import com.voximplant.sdkdemo.utils.NotificationHelper;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.util.concurrent.Executors;

public class SDKDemoApplication extends Application {

    private VoxClientManager mClientManager;
    private VoxCallManager mCallManager;
    private static Context mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();
        ForegroundCheck.init(this);
        SharedPreferencesHelper.init(getApplicationContext());
        NotificationHelper.init(getApplicationContext());
        mAppContext = getApplicationContext();

        ClientConfig clientConfig = new ClientConfig();
        //clientConfig.enableDebugLogging = true;
        //clientConfig.H264first = true;
        IClient client = Voximplant.getClientInstance(Executors.newSingleThreadExecutor(), getApplicationContext(), clientConfig);
        mClientManager = new VoxClientManager(client, getApplicationContext());
        mCallManager = new VoxCallManager(client, getApplicationContext());
    }

    public VoxClientManager getClientManager() {
        return mClientManager;
    }

    public VoxCallManager getCallManager() {
        return mCallManager;
    }

    public static Context getAppContext() {
        return mAppContext;
    }

}
