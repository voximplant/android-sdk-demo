/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.incomingcall;

import com.voximplant.sdkdemo.BasePresenter;
import com.voximplant.sdkdemo.BaseView;

public interface IncomingCallContract {
    interface View extends BaseView<Presenter> {
        void onCallEnded(String callId);
    }

    interface Presenter extends BasePresenter {
        boolean isVideoCall();
        String getCallId();
        void answerCall();
        void rejectCall();
    }
}
