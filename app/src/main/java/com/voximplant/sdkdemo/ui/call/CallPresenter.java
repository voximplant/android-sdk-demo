/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.call;

import android.util.Log;

import com.voximplant.sdk.Voximplant;
import com.voximplant.sdk.call.CallException;
import com.voximplant.sdk.call.CallSettings;
import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.call.ICallCompletionHandler;
import com.voximplant.sdk.call.ICallListener;
import com.voximplant.sdk.call.IEndpoint;
import com.voximplant.sdk.call.IEndpointListener;
import com.voximplant.sdk.call.IVideoStream;
import com.voximplant.sdk.call.RenderScaleType;
import com.voximplant.sdk.call.VideoFlags;
import com.voximplant.sdk.hardware.AudioDevice;
import com.voximplant.sdk.hardware.IAudioDeviceEventsListener;
import com.voximplant.sdk.hardware.IAudioDeviceManager;
import com.voximplant.sdk.hardware.ICameraEventsListener;
import com.voximplant.sdk.hardware.ICameraManager;
import com.voximplant.sdk.hardware.VideoQuality;
import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.Shared;
import com.voximplant.sdkdemo.manager.VoxCallManager;

import org.webrtc.SurfaceViewRenderer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.voximplant.sdkdemo.utils.Constants.APP_TAG;

public class CallPresenter implements CallContract.Presenter, ICallListener,
        ICameraEventsListener, IAudioDeviceEventsListener, IEndpointListener {
    private WeakReference<CallContract.View> mView;
    private WeakReference<ICall> mCall;
    private VoxCallManager mCallManager = Shared.getInstance().getCallManager();
    private ICameraManager mCameraManager;
    private IAudioDeviceManager mAudioDeviceManager;

    private boolean mIsIncoming;
    private boolean mIsVideoSent;
    private boolean mIsVideoReceived;
    private boolean mIsAudioMuted;
    private boolean mIsCallHeld;

    private int mCameraType;
    private VideoQuality mVideoQuality = VideoQuality.VIDEO_QUALITY_MEDIUM;

    private IVideoStream mLocalVideoStream;
    private HashMap<IVideoStream, IEndpoint> mEndpointVideoStreams = new HashMap<>();

    CallPresenter(CallContract.View view, String callId, boolean isIncoming, boolean withVideo) {
        mView = new WeakReference<>(view);
        mCall = new WeakReference<>(mCallManager.getCallById(callId));
        mIsIncoming = isIncoming;
        mIsVideoSent = withVideo;
        mIsVideoReceived = withVideo;
        mCameraType = 1;

        mCameraManager = Voximplant.getCameraManager(((CallFragment)mView.get()).getContext());
        mCameraManager.setCamera(mCameraType, mVideoQuality);
        mAudioDeviceManager = Voximplant.getAudioDeviceManager();
    }

    @Override
    public void start() {
        ICall call = mCall.get();
        if (call == null) {
            Log.e(APP_TAG, "CallPresenter: start: call is invalid");
            return;
        }

        // the listeners are removed on call disconnected/failed
        setupListeners(true);
        if (mIsIncoming) {
            try {
                CallSettings callSettings = new CallSettings();
                callSettings.videoFlags = new VideoFlags(mIsVideoReceived, mIsVideoSent);
                call.answer(callSettings);
            } catch (CallException e) {
                Log.e(APP_TAG, "CallPresenter: answer exception: " + e.getMessage());
                CallContract.View view = mView.get();
                if (view != null) {
                    view.callDisconnected();
                }
            }
        } else {
            try {
                call.start();
            } catch (CallException e) {
                Log.e(APP_TAG, "CallPresenter: startException: " + e.getMessage());
                CallContract.View view = mView.get();
                if (view != null) {
                    view.callDisconnected();
                }
            }
        }
        CallContract.View view = mView.get();
        if (view != null) {
            view.showVideoControls(mIsVideoSent);
            view.updateSendVideoCheckbox(mIsVideoSent);
            view.updateReceiveVideoCheckbox(mIsVideoReceived);
        }
    }

    private void setupListeners(boolean set) {
        ICall call = mCall.get();
        if (call == null) {
            Log.e(APP_TAG, "CallPresenter: start: call is invalid");
            return;
        }
        IEndpoint endpoint = null;
        if (call.getEndpoints().size() > 0) {
            endpoint = call.getEndpoints().get(0);
        }
        if (set) {
            if (endpoint != null) {
                endpoint.setEndpointListener(this);
            }
            call.addCallListener(this);
            mCameraManager.addCameraEventsListener(this);
            mAudioDeviceManager.addAudioDeviceEventsListener(this);
        } else {
            if (endpoint != null) {
                endpoint.setEndpointListener(null);
            }
            call.removeCallListener(this);
            mCameraManager.removeCameraEventsListener(this);
            mAudioDeviceManager.removeAudioDeviceEventsListener(this);
        }
    }

    @Override
    public void muteAudio() {
        ICall call = mCall.get();
        if (call != null) {
            mIsAudioMuted = !mIsAudioMuted;
            call.sendAudio(!mIsAudioMuted);
            CallContract.View view = mView.get();
            if (view != null) {
                view.updateMicButton(mIsAudioMuted);
            }
        }
    }

    @Override
    public void sendVideo(boolean send) {
        ICall call = mCall.get();
        if (call == null) {
            return;
        }
        final boolean doSend = send && !mIsVideoSent;
        call.sendVideo(doSend, new ICallCompletionHandler() {
            @Override
            public void onComplete() {
                mIsVideoSent = doSend;
                CallContract.View view = mView.get();
                if (view != null) {
                    view.showVideoControls(doSend);
                    view.updateSendVideoCheckbox(doSend);
                }
            }

            @Override
            public void onFailure(CallException exception) {
                CallContract.View view = mView.get();
                if (view != null) {
                    view.showError(R.string.toast_midcall_operation_failed, "Send video (" + doSend + ")", exception.getErrorCode().toString());
                }
            }
        });
    }

    @Override
    public void receiveVideo() {
        ICall call = mCall.get();
        if (call == null) {
            return;
        }
        call.receiveVideo(new ICallCompletionHandler() {
            @Override
            public void onComplete() {
                mIsVideoReceived = !mIsVideoReceived;
                CallContract.View view = mView.get();
                if (view != null) {
                    view.updateReceiveVideoCheckbox(mIsVideoReceived);
                }
            }

            @Override
            public void onFailure(CallException exception) {
                CallContract.View view = mView.get();
                if (view != null) {
                    view.showError(R.string.toast_midcall_operation_failed, "Receive video", exception.getErrorCode().toString());
                    view.updateReceiveVideoCheckbox(mIsVideoReceived);
                }
            }
        });
    }

    @Override
    public void hold() {
        ICall call = mCall.get();
        if (call == null) {
            return;
        }
        call.hold(!mIsCallHeld, new ICallCompletionHandler() {
            @Override
            public void onComplete() {
                mIsCallHeld = !mIsCallHeld;
                CallContract.View view = mView.get();
                if (view != null) {
                    view.updateHoldButton(mIsCallHeld);
                }
            }

            @Override
            public void onFailure(CallException exception) {
                CallContract.View view = mView.get();
                if (view != null) {
                    view.showError(R.string.toast_midcall_operation_failed, "Hold (" + !mIsCallHeld + ")", exception.getErrorCode().toString());
                }
            }
        });
    }

    @Override
    public void stopCall() {
        ICall call = mCall.get();
        if (call != null) {
            call.hangup(null);
        }
    }

    @Override
    public void localVideoViewCreated(SurfaceViewRenderer renderer) {
        if (mLocalVideoStream != null) {
            mLocalVideoStream.addVideoRenderer(renderer, RenderScaleType.SCALE_FIT);
        }
    }

    @Override
    public void localVideoViewRemoved(SurfaceViewRenderer renderer) {
        if (mLocalVideoStream != null) {
            mLocalVideoStream.removeVideoRenderer(renderer);
            mLocalVideoStream = null;
        }
    }

    @Override
    public synchronized void remoteVideoViewCreated(String streamId, SurfaceViewRenderer renderer) {
        for (Map.Entry<IVideoStream, IEndpoint> entry : mEndpointVideoStreams.entrySet()) {
            if (entry.getKey().getVideoStreamId().equals(streamId)) {
                entry.getKey().addVideoRenderer(renderer, RenderScaleType.SCALE_FIT);
            }
        }
    }

    @Override
    public synchronized void remoteVideoViewRemoved(String streamId, SurfaceViewRenderer renderer) {
        IVideoStream videoStream = null;
        for (Map.Entry<IVideoStream, IEndpoint> entry : mEndpointVideoStreams.entrySet()) {
            if (entry.getKey().getVideoStreamId().equals(streamId)) {
                entry.getKey().removeVideoRenderer(renderer);
                videoStream = entry.getKey();
            }
        }
        if (videoStream != null) {
            mEndpointVideoStreams.remove(videoStream);
        }
    }

    @Override
    public void switchCamera() {
        int newCameraType = (mCameraType == 0) ? 1 : 0;
        mCameraManager.setCamera(newCameraType, mVideoQuality);
    }

    @Override
    public List<String> getAudioDevices() {
        final List<String> audioDevices = new ArrayList<>();
        for (AudioDevice device : mAudioDeviceManager.getAudioDevices()) {
            audioDevices.add(device.toString());
        }
        return audioDevices;
    }

    //region Camera Events
    @Override
    public void onCameraSwitchDone(boolean isFrontCamera) {
        mCameraType = isFrontCamera ? 1 : 0;
        CallContract.View view = mView.get();
        if (view != null) {
            view.updateCameraButton(isFrontCamera);
        }
    }

    @Override
    public void onCameraSwitchError(String errorDescription) {
        CallContract.View view = mView.get();
        if (view != null) {
            view.showError(R.string.toast_midcall_operation_failed, "Camera switch", errorDescription);
        }
    }
    //endregion

    //region Call Events
    @Override
    public void onCallConnected(ICall call, Map<String, String> headers) {
        mCallManager.startForegroundCallService();
        Log.i(APP_TAG, "onCallConnected: " + call.getCallId());
        CallContract.View view = mView.get();
        if (view != null) {
            view.updateCallStatus(call.isVideoEnabled() ? -1 : R.string.audio_call_in_progress);
        }
    }

    @Override
    public void onCallDisconnected(ICall call, Map<String, String> headers, boolean answeredElsewhere) {
        mCallManager.removeCall(call.getCallId());
        mCallManager.stopForegroundService();
        Log.i(APP_TAG, "onCallDisconnected: " + call.getCallId());
        setupListeners(false);
        CallContract.View view = mView.get();
        if (view != null) {
            view.removeAllVideoViews();
            view.callDisconnected();
        }
    }

    @Override
    public void onCallAudioStarted(ICall call) {
        Log.i(APP_TAG, "onCallAudioStarted: " + call.getCallId());
        CallContract.View view = mView.get();
        if (view != null) {
            view.updateCallStatus(R.string.audio_is_started);
        }
    }

    @Override
    public void onCallRinging(ICall call, Map<String, String> headers) {
        Log.i(APP_TAG, "onCallRinging: " + call.getCallId());
        CallContract.View view = mView.get();
        if (view != null) {
            view.updateCallStatus(R.string.call_ringing);
        }
    }

    @Override
    public void onCallFailed(ICall call, int code, final String description, Map<String, String> headers) {
        Log.i(APP_TAG, "onCallFailed: " + call.getCallId() + ", code: " + code);
        setupListeners(false);
        if (code != 409) {
            CallContract.View view = mView.get();
            if (view != null) {
                view.callFailed(description);
            }
        }
    }

    @Override
    public void onLocalVideoStreamAdded(ICall call, IVideoStream videoStream) {
        Log.i(APP_TAG, "onLocalVideoStreamAdded: " + call.getCallId());
        mLocalVideoStream = videoStream;
        CallContract.View view = mView.get();
        if (view != null) {
            view.createLocalVideoView();
        }
    }

    @Override
    public void onLocalVideoStreamRemoved(ICall call, IVideoStream videoStream) {
        Log.i(APP_TAG, "onLocalVideoStreamRemoved: " + call.getCallId());
        CallContract.View view = mView.get();
        if (view != null) {
            view.removeLocalVideoView();
        }
    }

    @Override
    public void onEndpointAdded(ICall call, IEndpoint endpoint) {
        Log.i(APP_TAG, "onEndpointAdded: " + call.getCallId() + " " + endpoint.getEndpointId());
        endpoint.setEndpointListener(this);
    }
    //endregion

    //region Audio Device events
    @Override
    public void onAudioDeviceChanged(AudioDevice currentAudioDevice) {
        CallContract.View view = mView.get();
        if (view != null) {
            view.updateAudioDeviceButton(currentAudioDevice);
        }
    }

    @Override
    public void onAudioDeviceListChanged(List<AudioDevice> list) {

    }
    //endregion

    //region Endpoint events
    @Override
    public synchronized void onRemoteVideoStreamAdded(IEndpoint endpoint, IVideoStream videoStream) {
        if (endpoint != null && videoStream != null) {
            Log.i(APP_TAG, "onRemoteVideoStreamAdded: "+ endpoint.getEndpointId() + ", stream: " + videoStream.getVideoStreamId());
            mEndpointVideoStreams.put(videoStream, endpoint);
            CallContract.View view = mView.get();
            if (view != null) {
                view.createRemoteVideoView(videoStream.getVideoStreamId(), endpoint.getUserDisplayName());
            }
        }
    }

    @Override
    public void onRemoteVideoStreamRemoved(IEndpoint endpoint, IVideoStream videoStream) {
        if (endpoint != null && videoStream != null) {
            Log.i(APP_TAG, "onRemoteVideoStreamRemoved: " + endpoint.getEndpointId() + ", stream: " + videoStream.getVideoStreamId());
            CallContract.View view = mView.get();
            if (view != null) {
                view.removeRemoteVideoView(videoStream.getVideoStreamId());
            }
        }
    }


    @Override
    public void onEndpointRemoved(IEndpoint endpoint) {
        ICall call = mCall.get();
        if (call != null) {
            Log.i(APP_TAG, "onEndpointRemoved: " + call.getCallId() + " " + endpoint.getEndpointId());
            endpoint.setEndpointListener(null);
        }
    }

    @Override
    public void onEndpointInfoUpdated(IEndpoint endpoint) {
        ICall call = mCall.get();
        if (call != null) {
            Log.i(APP_TAG, "onEndpointInfoUpdated: " + call.getCallId() + " " + endpoint.getEndpointId());
            for (IVideoStream stream : endpoint.getVideoStreams()) {
                CallContract.View view = mView.get();
                if (view != null) {
                    view.updateRemoteVideoView(stream.getVideoStreamId(), endpoint.getUserDisplayName());
                }
            }
        }
    }
    //endregion
}
