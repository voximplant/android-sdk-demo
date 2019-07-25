/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.incomingcall;

import android.util.Log;

import com.voximplant.sdk.call.CallException;
import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.call.ICallListener;
import com.voximplant.sdk.call.RejectMode;
import com.voximplant.sdkdemo.Shared;
import com.voximplant.sdkdemo.manager.VoxCallManager;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import static com.voximplant.sdkdemo.utils.Constants.APP_TAG;

public class IncomingCallPresenter implements IncomingCallContract.Presenter, ICallListener {
    private WeakReference<IncomingCallContract.View> mView;
    private WeakReference<ICall> mCall;
    private HashMap<String, String> mHeaders = null;

    IncomingCallPresenter(IncomingCallContract.View view, String callId) {
        mView = new WeakReference<>(view);
        VoxCallManager callManager = Shared.getInstance().getCallManager();
        if (callId != null && callManager != null) {
            ICall call = callManager.getCallById(callId);
            if (call != null) {
                mCall = new WeakReference<>(call);
                call.addCallListener(this);
            }
        } else {
            Log.e(APP_TAG, "IncomingCallPresenter: failed to get call by id");
        }
    }

    @Override
    public void start() {}

    private void stop() {
        ICall call = mCall.get();
        if (call != null) {
            call.removeCallListener(this);
            IncomingCallContract.View view = mView.get();
            if (view != null) {
                view.onCallEnded(call.getCallId());
            }
        }
    }

    @Override
    public boolean isVideoCall() {
        return mCall.get() != null && mCall.get().isVideoEnabled();
    }

    @Override
    public String getCallId() {
        return mCall != null ? mCall.get().getCallId() : null;
    }

    @Override
    public void answerCall() {
        ICall call = mCall.get();
        if (call != null) {
            call.removeCallListener(this);
        }
    }

    @Override
    public void rejectCall() {
        ICall call = mCall.get();
        if (call == null) {
            Log.e(APP_TAG, "IncomingCallPresenter: rejectCall: invalid call");
            return;
        }
        try {
            call.reject(RejectMode.DECLINE, mHeaders);
        } catch (CallException e) {
            Log.e(APP_TAG, "IncomingCallPresenter: reject call exception: " + e.getMessage());
            stop();
        }
    }

    @Override
    public void onCallFailed(ICall call, int code, String description, Map<String, String> headers) {
        stop();
    }

    @Override
    public void onCallDisconnected(ICall call, Map<String, String> headers, boolean answeredElsewhere) {
        stop();
    }

}
