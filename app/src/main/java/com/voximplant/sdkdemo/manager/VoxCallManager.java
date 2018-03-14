/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.manager;

import android.content.Context;
import android.content.Intent;

import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.call.ICallListener;
import com.voximplant.sdk.call.IVideoStream;
import com.voximplant.sdk.call.VideoFlags;
import com.voximplant.sdk.client.IClient;
import com.voximplant.sdk.client.IClientIncomingCallListener;
import com.voximplant.sdkdemo.ui.incomingcall.IncomingCallActivity;

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
    private ConcurrentHashMap<String, ICallEventsListener> mCallEventsListeners = new ConcurrentHashMap<>();

    public VoxCallManager(IClient client, Context appContext) {
        mClient = client;
        mAppContext = appContext;
        mClient.setClientIncomingCallListener(this);
    }

    public void addCallEventListener(String callId, ICallEventsListener listener) {
        ICall call = mCalls.get(callId);
        if (call != null) {
            call.addCallListener(this);
            mCallEventsListeners.put(callId, listener);
        }
    }

    public void removeCallEventListener(String callId, ICallEventsListener listener) {
        ICall call = mCalls.get(callId);
        if (call != null) {
            call.removeCallListener(this);
            mCallEventsListeners.remove(callId, listener);
        }
    }

    public String createCall(String user, VideoFlags videoFlags) {
        ICall call = mClient.callTo(user, videoFlags, null);
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
        if (mCalls.containsKey(callId)) {
            mCalls.remove(callId);
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


    @Override
    public void onCallConnected(ICall call, Map<String, String> headers) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onCallConnected(headers);
        }
    }

    @Override
    public void onCallDisconnected(ICall call, Map<String, String> headers, boolean answeredElsewhere) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onCallDisconnected(headers, answeredElsewhere);
        }
        removeCall(call.getCallId());
    }

    @Override
    public void onCallRinging(ICall call, Map<String, String> headers) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onCallRinging(headers);
        }
    }

    @Override
    public void onCallFailed(ICall call, int code, String description, Map<String, String> headers) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onCallFailed(code, description, headers);
        }
        removeCall(call.getCallId());
    }

    @Override
    public void onCallAudioStarted(ICall call) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onCallAudioStarted();
        }
    }

    @Override
    public void onSIPInfoReceived(ICall call, String type, String content, Map<String, String> headers) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onSIPInfoReceived(type, content, headers);
        }
    }

    @Override
    public void onMessageReceived(ICall call, String text) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onMessageReceived(text);
        }
    }

    @Override
    public void onLocalVideoStreamAdded(ICall call, IVideoStream videoStream) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onLocalVideoStreamAdded(videoStream);
        }
    }

    @Override
    public void onLocalVideoStreamRemoved(ICall call, IVideoStream videoStream) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onLocalVideoStreamRemoved(videoStream);
        }
    }

    @Override
    public void onICETimeout(ICall call) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onICETimeout();
        }
    }

    @Override
    public void onICECompleted(ICall call) {
        ICallEventsListener listener = mCallEventsListeners.get(call.getCallId());
        if (listener != null) {
            listener.onICECompleted();
        }
    }
}
