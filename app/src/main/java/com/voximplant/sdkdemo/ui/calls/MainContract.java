/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.calls;

import com.voximplant.sdkdemo.BasePresenter;
import com.voximplant.sdkdemo.BaseView;

public interface MainContract {
    interface View extends BaseView<Presenter> {
        void notifyConnectionClosed();
        void notifyInvalidCallUser();

        void startCallFragment(String callId, boolean withVideo, String user, boolean isIncoming);
        void removeCallFragment(String callId);

        boolean checkPermissionsGrantedForCall(boolean isVideoCall);
    }

    interface Presenter extends BasePresenter {
        void answerCall(String callId, boolean withVideo);
        void makeCall(String user, boolean withVideo);

        void permissionsAreGrantedForCall();

        void logout();
    }
}
