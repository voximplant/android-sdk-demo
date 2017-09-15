/*
 * Copyright (c) 2017, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.voximplant.sdk.call.CallException;
import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.call.ICallCompletionHandler;
import com.voximplant.sdk.call.ICallListener;
import com.voximplant.sdk.call.IEndpoint;
import com.voximplant.sdk.call.IEndpointListener;
import com.voximplant.sdk.call.RenderScaleType;
import com.voximplant.sdk.call.IVideoStream;
import com.voximplant.sdk.Voximplant;
import com.voximplant.sdk.call.VideoFlags;
import com.voximplant.sdk.hardware.ICameraEventsListener;
import com.voximplant.sdk.hardware.ICameraManager;
import com.voximplant.sdk.hardware.VideoQuality;
import com.voximplant.sdkdemo.R;

import org.webrtc.SurfaceViewRenderer;

import java.util.HashMap;
import java.util.Map;

import static com.voximplant.sdkdemo.utils.Constants.CALL_ID;
import static com.voximplant.sdkdemo.utils.Constants.INCOMING_CALL;
import static com.voximplant.sdkdemo.utils.Constants.WITH_VIDEO;

public class CallFragment extends Fragment implements ICallListener, IEndpointListener, ICameraEventsListener {
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;

    private String callId;
    private ICall mCall;
    private IEndpoint mEndpoint;
    private boolean mIsIncomingCall;
    private boolean mAnswerWithVideo;
    private HashMap<SurfaceViewRenderer, IVideoStream> mRemoteVideoStreamRenders = new HashMap<>();

    private PercentFrameLayout mLocalRenderLayout;
    private PercentFrameLayout mRemoteRenderLayout;
    private SurfaceViewRenderer mLocalRender;
    private SurfaceViewRenderer mRemoteRender;

    private ImageButton mHangupButton;
    private ImageButton mMoreSettingsButton;
    private ImageButton mSwitchCameraButton;
    private ImageButton mMuteAudioButton;
    private ImageButton mHoldButton;
    private ImageButton mSpeakerPhoneButton;

    private CheckBox mReceiveVideoCheckBox;
    private CheckBox mSendVideoCheckBox;

    private AlertDialog mAlertDialog;
    private TextView mCallStatusTextView;
    private ICameraManager mCameraManager;

    private boolean mIsAudioMuted;
    private boolean mIsVideoSent;
    private boolean mIsVideoReceived;
    private boolean mIsSpeakerPhoneEnabled;
    private boolean mIsCallHeld;
    private int mCameraType;

    private OnCallFragmentListener mListener;

    public CallFragment() {
        // Required empty public constructor
    }

    public static CallFragment newInstance(String callId, boolean isIncomingCall, boolean withVideo) {
        CallFragment fragment = new CallFragment();
        Bundle args = new Bundle();
        args.putString(CALL_ID, callId);
        args.putBoolean(INCOMING_CALL, isIncomingCall);
        args.putBoolean(WITH_VIDEO, withVideo);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            callId = getArguments().getString(CALL_ID);
            mIsIncomingCall = getArguments().getBoolean(INCOMING_CALL);
            mAnswerWithVideo = getArguments().getBoolean(WITH_VIDEO);
        }
        mCameraType = 1; //front camera 1, back camera 0
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call, container, false);

        mCameraManager = Voximplant.getCameraManager(getContext());
        mCameraManager.addCameraEventsListener(this);

        mLocalRender = (SurfaceViewRenderer) view.findViewById(R.id.local_video_view);
        mRemoteRender = (SurfaceViewRenderer) view.findViewById(R.id.remote_video_view);
        mLocalRenderLayout = (PercentFrameLayout) view.findViewById(R.id.local_video_layout);
        mRemoteRenderLayout = (PercentFrameLayout) view.findViewById(R.id.remote_video_layout);

        mLocalRender.setZOrderMediaOverlay(true);

        mRemoteRenderLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        mLocalRenderLayout.setPosition(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);

        mCallStatusTextView = (TextView) view.findViewById(R.id.call_status_view);

        mHangupButton = (ImageButton) view.findViewById(R.id.hangup_button);
        mHangupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCall != null) {
                    mCall.hangup(null);
                }
            }
        });

        mMuteAudioButton = (ImageButton) view.findViewById(R.id.mute_audio_button);
        mMuteAudioButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsAudioMuted = !mIsAudioMuted;
                if (mCall != null) {
                    mCall.sendAudio(!mIsAudioMuted);
                }
                if (mIsAudioMuted) {
                    mMuteAudioButton.setBackgroundResource(R.mipmap.ic_mic_on);
                } else {
                    mMuteAudioButton.setBackgroundResource(R.mipmap.ic_mic_off);
                }
            }
        });

        mSwitchCameraButton = (ImageButton) view.findViewById(R.id.switch_camera_button);
        if (mCall != null && !mCall.isVideoEnabled()) {
            mSwitchCameraButton.setVisibility(View.INVISIBLE);
        }
        mSwitchCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCameraType = (mCameraType == 0) ? 1 : 0;
                mCameraManager.setCamera(mCameraType, VideoQuality.VIDEO_QUALITY_MEDIUM);
            }
        });

        final RelativeLayout moreSettingsLayout = (RelativeLayout) view.findViewById(R.id.more_settings_layout);
        mMoreSettingsButton = (ImageButton) view.findViewById(R.id.more_settings_button);
        mMoreSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (moreSettingsLayout.isShown()) {
                    moreSettingsLayout.setVisibility(View.INVISIBLE);
                } else {
                    moreSettingsLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        if (mIsIncomingCall) {
            if (mAnswerWithVideo) {
                mIsVideoSent = true;
                mIsVideoReceived = true;
            }
        } else if (mCall.isVideoEnabled()) {
            mIsVideoSent = true;
            mIsVideoReceived = true;
        }

        mSendVideoCheckBox = (CheckBox) view.findViewById(R.id.send_video_checkbox);
        mSendVideoCheckBox.setChecked(mIsVideoSent);
        mSendVideoCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsVideoSent = !mIsVideoSent;
                mCall.sendVideo(isChecked && mIsVideoSent, new ICallCompletionHandler() {
                    @Override
                    public void onComplete() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mIsVideoSent) {
                                    mSwitchCameraButton.setVisibility(View.VISIBLE);
                                    addVideo();
                                } else {
                                    mSwitchCameraButton.setVisibility(View.INVISIBLE);
                                    removeVideo();
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(CallException exception) {

                    }
                });
            }
        });

        mReceiveVideoCheckBox = (CheckBox) view.findViewById(R.id.receive_video_checkbox);
        mReceiveVideoCheckBox.setChecked(mIsVideoReceived);
        if (mIsVideoReceived) {
            mReceiveVideoCheckBox.setEnabled(false);
        }
        mReceiveVideoCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mIsVideoReceived = !mIsVideoReceived;
                mCall.receiveVideo(new ICallCompletionHandler() {
                    @Override
                    public void onComplete() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mReceiveVideoCheckBox.setEnabled(false);
                            }
                        });
                    }

                    @Override
                    public void onFailure(CallException exception) {

                    }
                });
            }
        });

        mHoldButton = (ImageButton) view.findViewById(R.id.hold_button);
        mHoldButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsCallHeld = !mIsCallHeld;
                mCall.hold(mIsCallHeld, new ICallCompletionHandler() {
                    @Override
                    public void onComplete() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (mIsCallHeld) {
                                    mHoldButton.setBackgroundResource(R.mipmap.ic_held);
                                } else {
                                    mHoldButton.setBackgroundResource(R.mipmap.ic_hold);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(CallException exception) {

                    }
                });
            }
        });

        mSpeakerPhoneButton = (ImageButton) view.findViewById(R.id.speaker_button);
        mSpeakerPhoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsSpeakerPhoneEnabled = !mIsSpeakerPhoneEnabled;
                Voximplant.getAudioDeviceManager().enableLoudspeaker(mIsSpeakerPhoneEnabled);
                if (mIsSpeakerPhoneEnabled) {
                    mSpeakerPhoneButton.setBackgroundResource(R.mipmap.ic_speaker_on);
                } else {
                    mSpeakerPhoneButton.setBackgroundResource(R.mipmap.ic_speaker_off);
                }
            }
        });

        for (String camera : mCameraManager.getCameraDeviceNames()) {
            Log.i("SDKDemoApplication", camera + ": " + mCameraManager.getCameraSupportedResolutions(camera).toString());
        }

        startCall();
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnCallFragmentListener) {
            mListener = (OnCallFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnCallFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (mCall != null) {
            mCall.removeCallListener(this);
            mEndpoint.setEndpointListener(null);
            mCameraManager.removeCameraEventsListener(this);
        }
        mCameraManager = null;
        mEndpoint = null;
        mCall = null;
        mListener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAlertDialog = null;
        mCallStatusTextView = null;
        mLocalRenderLayout = null;
        mRemoteRenderLayout = null;
        mLocalRender = null;
        mRemoteRender = null;
        mHangupButton = null;
        mMoreSettingsButton = null;
        mSwitchCameraButton = null;
        mMuteAudioButton = null;
        mHoldButton = null;
        mSpeakerPhoneButton = null;
    }

    @Override
    public void onHiddenChanged (boolean hidden) {
        if (hidden) {
            mLocalRender.setVisibility(View.INVISIBLE);
            mRemoteRender.setVisibility(View.INVISIBLE);
        } else {
            mLocalRender.setVisibility(View.VISIBLE);
            mRemoteRender.setVisibility(View.VISIBLE);
        }
    }

    public void setCall(ICall call) {
        mCall = call;
        if (mCall != null) {
            mCall.addCallListener(this);
            mEndpoint = mCall.getEndpoints().get(0);
            mEndpoint.setEndpointListener(this);
        }
    }

    public void endCall() {
        if (mCall != null) {
            mCall.hangup(null);
        }
    }

    private void startCall() {
        if ((mIsIncomingCall && mAnswerWithVideo) || (!mIsIncomingCall && mCall.isVideoEnabled())) {
            addVideo();
        }
        mCallStatusTextView.setText(getString(R.string.call_connecting));
        if (mIsIncomingCall) {
            try {
                if (mCall != null) {
                    mCall.answer("test custom data android new API", new VideoFlags(mIsVideoReceived, mIsVideoSent), null);
                }
            } catch (CallException e) {
                Log.e("SDKDemoApplication", "CallFragment: answer exception: " + e);
            }
        } else {
            if (mCall != null) {
                try {
                    mCall.start(null);
                } catch (CallException e) {
                    Log.e("SDKDemoApplication", "CallFragment: start exception: " + e.getMessage());
                }
            }
        }
    }

    private void addVideo() {
        mCameraManager.setCamera(mCameraType, VideoQuality.VIDEO_QUALITY_MEDIUM);
        mCallStatusTextView.setText("");
        mLocalRender.setVisibility(View.VISIBLE);
    }

    private void removeVideo() {
        mLocalRender.setVisibility(View.INVISIBLE);
    }

    // ICall callbacks
    @Override
    public void onCallConnected(ICall iCall, Map<String, String> headers) {
        Log.i("SDKDemoApplication", "onCallConnected");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mCall != null && !mCall.isVideoEnabled()) {
                    mCallStatusTextView.setText(getText(R.string.audio_call_in_progress));
                } else {
                    mCallStatusTextView.setText("");
                }
            }
        });
    }

    @Override
    public void onCallDisconnected(ICall iCall, Map<String, String> headers, boolean answeredElsewhere) {
        Log.i("SDKDemoApplication", "onCallDisconnected");
        if (mListener != null) {
            mListener.onCallDisconnected(callId);
        }
    }

    @Override
    public void onCallRinging(ICall iCall, Map<String, String> headers) {
        Log.i("SDKDemoApplication", "onCallRinging");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCallStatusTextView.setText(getText(R.string.call_ringing));
            }
        });
    }

    @Override
    public void onCallFailed(ICall iCall, int code, final String description, Map<String, String> headers) {
        Log.i("SDKDemoApplication", "onCallFailed");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mAlertDialog = new AlertDialog.Builder(getContext())
                        .setTitle(R.string.call_failed)
                        .setMessage("Reason: " + description)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (mListener != null) {
                                    mListener.onCallDisconnected(callId);
                                }
                            }
                        })
                        .show();
                mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        if (mListener != null) {
                            mListener.onCallDisconnected(callId);
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onCallAudioStarted(ICall iCall) {
        Log.i("SDKDemoApplication", "onCallAudioStarted");
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mCallStatusTextView.setText(getText(R.string.audio_is_started));
            }
        });
    }

    @Override
    public void onLocalVideoStreamAdded(ICall call, IVideoStream videoStream) {
        videoStream.addVideoRenderer(mLocalRender, RenderScaleType.SCALE_FIT);
    }

    @Override
    public void onLocalVideoStreamRemoved(ICall call, IVideoStream videoStream) {
        videoStream.removeVideoRenderer(mLocalRender);
    }

    @Override
    public void onICETimeout(ICall call) {
        Log.i("SDKDemoApplication", "onICETimeout");
    }

    @Override
    public void onICECompleted(ICall call) {
        Log.i("SDKDemoApplication", "onICECompleted");
    }

    @Override
    public void onRemoteVideoStreamAdded(IEndpoint endpoint, IVideoStream videoStream) {
        if (mRemoteVideoStreamRenders.containsKey(mRemoteRender)) {
            IVideoStream currentRenderedRemoteVideoStream = mRemoteVideoStreamRenders.get(mRemoteRender);
            if (currentRenderedRemoteVideoStream != null) {
                currentRenderedRemoteVideoStream.removeVideoRenderer(mRemoteRender);
                mRemoteVideoStreamRenders.remove(mRemoteRender);
            }
        }
        videoStream.addVideoRenderer(mRemoteRender, RenderScaleType.SCALE_FIT);
        mRemoteVideoStreamRenders.put(mRemoteRender, videoStream);
    }

    @Override
    public void onRemoteVideoStreamRemoved(IEndpoint endpoint, IVideoStream videoStream) {
        if (mRemoteVideoStreamRenders.containsValue(videoStream)) {
            videoStream.removeVideoRenderer(mRemoteRender);
            mRemoteVideoStreamRenders.remove(mRemoteRender);
        }
    }

    @Override
    public void onSIPInfoReceived(ICall iCall, String type, String content, Map<String, String> headers) {

    }

    @Override
    public void onMessageReceived(ICall iCall, String text) {

    }

    @Override
    public void onCameraError(String errorDescription) {

    }

    @Override
    public void onCameraDisconnected() {

    }

    @Override
    public void onCameraSwitchDone(final boolean isFrontCamera) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isFrontCamera) {
                    mSwitchCameraButton.setBackgroundResource(R.mipmap.ic_camera_back);
                    mCameraType = 1;
                } else {
                    mSwitchCameraButton.setBackgroundResource(R.mipmap.ic_camera_front);
                    mCameraType = 0;
                }
            }
        });
    }

    @Override
    public void onCameraSwitchError(String errorDescription) {

    }

    public interface OnCallFragmentListener {
        void onCallDisconnected(String callId);
    }
}
