/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.manager;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.voximplant.sdk.call.CallSettings;
import com.voximplant.sdk.call.CallStats;
import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.call.ICallListener;
import com.voximplant.sdk.call.IEndpoint;
import com.voximplant.sdk.call.IVideoStream;
import com.voximplant.sdk.call.VideoFlags;
import com.voximplant.sdk.client.IClient;
import com.voximplant.sdk.client.IClientIncomingCallListener;
import com.voximplant.sdkdemo.ui.call.CallService;
import com.voximplant.sdkdemo.ui.incomingcall.IncomingCallActivity;
import com.voximplant.sdkdemo.utils.Constants;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.voximplant.sdkdemo.utils.Constants.CALL_ID;
import static com.voximplant.sdkdemo.utils.Constants.DISPLAY_NAME;
import static com.voximplant.sdkdemo.utils.Constants.WITH_VIDEO;

public class VoxCallManager implements IClientIncomingCallListener, ICallListener {
    private HashMap<String, ICall> mCalls = new HashMap<>();
    private final IClient mClient;
    private final Context mAppContext;

    public VoxCallManager(IClient client, Context appContext) {
        mClient = client;
        mAppContext = appContext;
        mClient.setClientIncomingCallListener(this);
    }

    public String createCall(String user, VideoFlags videoFlags) {
        CallSettings callSettings = new CallSettings();
        callSettings.videoFlags = videoFlags;
        ICall call = mClient.call(user, callSettings);
        if (call != null) {
            mCalls.put(call.getCallId(), call);
            return call.getCallId();
        }
        return null;
    }

    public void endAllCalls() {
        for (Map.Entry<String, ICall> entry : mCalls.entrySet()) {
            entry.getValue().hangup(null);
        }
    }

    public ICall getCallById(String callId) {
        if (mCalls.containsKey(callId)) {
            return mCalls.get(callId);
        }
        return null;
    }

    public void removeCall(String callId) {
        mCalls.remove(callId);
    }

    public void startForegroundCallService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent intent = new Intent(mAppContext, CallService.class);
            intent.setAction(Constants.ACTION_FOREGROUND_SERVICE_START);
            intent.putExtra(Constants.SERVICE_NOTIFICATION_DETAILS, mCalls.size());
            mAppContext.startForegroundService(intent);
        }
    }

    public void stopForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (mCalls.isEmpty()) {
                Intent intent = new Intent(mAppContext, CallService.class);
                intent.setAction(Constants.ACTION_FOREGROUND_SERVICE_STOP);
                mAppContext.stopService(intent);
            } else {
                // Do not stop foreground service if there are other calls in progress
                // Just update the notification
                startForegroundCallService();
            }
        }
    }

    @Override
    public void onIncomingCall(ICall call, boolean video,  Map<String, String> headers) {
        mCalls.put(call.getCallId(), call);
        Intent incomingCallIntent = new Intent(mAppContext, IncomingCallActivity.class);
        incomingCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        incomingCallIntent.putExtra(CALL_ID, call.getCallId());
        incomingCallIntent.putExtra(WITH_VIDEO, video);
        incomingCallIntent.putExtra(DISPLAY_NAME, call.getEndpoints().get(0).getUserDisplayName());
        mAppContext.startActivity(incomingCallIntent);
    }
}
