/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.calls;

import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.call.VideoFlags;
import com.voximplant.sdkdemo.Shared;
import com.voximplant.sdkdemo.manager.IClientManagerListener;
import com.voximplant.sdkdemo.manager.VoxCallManager;
import com.voximplant.sdkdemo.manager.VoxClientManager;

import java.lang.ref.WeakReference;

public class MainPresenter implements MainContract.Presenter, IClientManagerListener {
    private class CallDescriptor {
        private String mCallId;
        private boolean mWithVideo;
        private String mUser;
        private boolean mIsIncoming;

        CallDescriptor(String callId, boolean withVideo, String user, boolean isIncoming) {
            mCallId = callId;
            mWithVideo = withVideo;
            mUser = user;
            mIsIncoming = isIncoming;
        }

        String getCallId() { return mCallId; }
        boolean getIsVideo() { return mWithVideo; }
        String getUser() { return mUser; }
        boolean getIsIncoming() { return mIsIncoming; }
    }

    private WeakReference<MainContract.View> mView;
    private VoxCallManager mCallManager = Shared.getInstance().getCallManager();
    private VoxClientManager mClientManager = Shared.getInstance().getClientManager();
    private CallDescriptor mCallWaitingPermissions = null;

    MainPresenter(MainContract.View view) {
        mView = new WeakReference<>(view);
    }

    @Override
    public void start() {
        Shared.getInstance().getClientManager().addListener(this);
    }

    @Override
    public void onConnectionClosed() {
        MainContract.View view = mView.get();
        if (view != null) {
            view.notifyConnectionClosed();
        }
    }

    @Override
    public void answerCall(String callId, boolean withVideo) {
        ICall call = mCallManager.getCallById(callId);
        if (call != null) {
            MainContract.View view = mView.get();
            if (view != null) {
                if (view.checkPermissionsGrantedForCall(withVideo)) {
                    view.startCallFragment(callId, withVideo, call.getEndpoints().get(0).getUserDisplayName(), true);
                } else {
                    mCallWaitingPermissions = new CallDescriptor(callId, withVideo, call.getEndpoints().get(0).getUserDisplayName(), true);
                }
            }

        }
    }

    @Override
    public void makeCall(String user, boolean withVideo) {
        MainContract.View view = mView.get();
        if (view == null) {
            return;
        }
        if (user == null || user.isEmpty()) {
            view.notifyInvalidCallUser();
            return;
        }
        String callId = mCallManager.createCall(user, new VideoFlags(withVideo, withVideo));
        if (view.checkPermissionsGrantedForCall(withVideo)) {
            view.startCallFragment(callId, withVideo, user, false);
        } else {
            mCallWaitingPermissions = new CallDescriptor(callId, withVideo, user, false);
        }
    }

    @Override
    public void permissionsAreGrantedForCall() {
        MainContract.View view = mView.get();
        if (mCallWaitingPermissions != null && view != null) {
            view.startCallFragment(mCallWaitingPermissions.getCallId(),
                    mCallWaitingPermissions.getIsVideo(),
                    mCallWaitingPermissions.getUser(),
                    mCallWaitingPermissions.getIsIncoming());
        }
    }


    @Override
    public void logout() {
        mCallManager.endAllCalls();
        mClientManager.removeListener(this);
        mClientManager.logout();
    }
}
