/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.call;

import com.voximplant.sdk.hardware.AudioDevice;
import com.voximplant.sdkdemo.BasePresenter;
import com.voximplant.sdkdemo.BaseView;

import org.webrtc.SurfaceViewRenderer;

import java.util.List;

public interface CallContract {
    interface View extends BaseView<Presenter> {
        void updateCallStatus(int resStatus);
        void updateMicButton(boolean pressed);
        void updateHoldButton(boolean pressed);
        void updateSendVideoCheckbox(boolean checked);
        void updateReceiveVideoCheckbox(boolean checked);
        void updateCameraButton(boolean isFront);
        void updateAudioDeviceButton(AudioDevice audioDevice);

        void createLocalVideoView();
        void removeLocalVideoView();

        void createRemoteVideoView(String streamId, String displayName);
        void removeRemoteVideoView(String streamId);
        void removeAllVideoViews();
        void updateRemoteVideoView(String streamId, String displayName);

        void showVideoControls(boolean show);
        void callDisconnected();
        void callFailed(String error);
        void showError(int resError, String param1, String param2);
    }

    interface Presenter extends BasePresenter {
        void muteAudio();
        void sendVideo(boolean send);
        void receiveVideo();
        void hold();
        void stopCall();

        void localVideoViewCreated(SurfaceViewRenderer renderer);
        void localVideoViewRemoved(SurfaceViewRenderer renderer);

        void remoteVideoViewCreated(String streamId, SurfaceViewRenderer renderer);
        void remoteVideoViewRemoved(String streamId, SurfaceViewRenderer renderer);

        void switchCamera();
        List<String> getAudioDevices();
    }
}
