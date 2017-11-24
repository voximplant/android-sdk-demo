/*
 * Copyright (c) 2017, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.manager;

import android.content.Context;
import android.content.Intent;

import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.call.VideoFlags;
import com.voximplant.sdk.client.IClient;
import com.voximplant.sdk.client.IClientIncomingCallListener;
import com.voximplant.sdkdemo.activities.IncomingCallActivity;

import java.util.HashMap;
import java.util.Map;

import static com.voximplant.sdkdemo.utils.Constants.CALL_ID;

public class VoxCallManager implements IClientIncomingCallListener{
    private HashMap<String, ICall> mIncomingCalls = new HashMap<>();
    private final IClient mClient;
    private final Context mAppContext;

    public VoxCallManager(IClient client, Context appContext) {
        mClient = client;
        mAppContext = appContext;
        mClient.setClientIncomingCallListener(this);
    }

    public ICall createCall(String user, VideoFlags videoFlags) {
        return mClient.callTo(user, videoFlags, null);
    }

    public ICall getCallById(String callId) {
        if (mIncomingCalls.containsKey(callId)) {
            return mIncomingCalls.get(callId);
        }
        return null;
    }

    public void removeCall(String callId) {
        if (mIncomingCalls.containsKey(callId)) {
            mIncomingCalls.remove(callId);
        }
    }

    @Override
    public void onIncomingCall(ICall call, boolean video,  Map<String, String> headers) {
        mIncomingCalls.put(call.getCallId(), call);
        Intent incomingCallIntent = new Intent(mAppContext, com.voximplant.sdkdemo.activities.IncomingCallActivity.class);
        incomingCallIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
        incomingCallIntent.putExtra(CALL_ID, call.getCallId());
        mAppContext.startActivity(incomingCallIntent);
    }
}
