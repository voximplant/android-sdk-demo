/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.call;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.voximplant.sdk.Voximplant;
import com.voximplant.sdk.hardware.AudioDevice;
import com.voximplant.sdkdemo.R;

import java.util.List;

import static com.voximplant.sdkdemo.utils.Constants.CALL_ID;
import static com.voximplant.sdkdemo.utils.Constants.INCOMING_CALL;
import static com.voximplant.sdkdemo.utils.Constants.WITH_VIDEO;

public class CallFragment extends Fragment implements CallContract.View {
    private String callId;
    private boolean mIsIncomingCall;
    private boolean mAnswerWithVideo;

    private ImageButton mHangupButton;
    private ImageButton mMoreSettingsButton;
    private ImageButton mSwitchCameraButton;
    private ImageButton mMuteAudioButton;
    private ImageButton mHoldButton;
    private ImageButton mAudioDeviceButton;

    private CheckBox mReceiveVideoCheckBox;
    private CheckBox mSendVideoCheckBox;

    private AlertDialog mAlertDialog;
    private TextView mCallStatusTextView;

    private OnCallFragmentListener mListener;
    // -------------------------------------------------------
    private CallContract.Presenter mPresenter;
    private VideoViewsHelper mVideoViewsHelper;

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

        mPresenter = new CallPresenter(this, callId, mIsIncomingCall, mAnswerWithVideo);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_call, container, false);

        RelativeLayout videoViewsLayout = view.findViewById(R.id.video_views);

        mCallStatusTextView = view.findViewById(R.id.call_status_view);
        mHangupButton = view.findViewById(R.id.hangup_button);
        mMuteAudioButton = view.findViewById(R.id.mute_audio_button);
        mHoldButton = view.findViewById(R.id.hold_button);
        mSendVideoCheckBox = view.findViewById(R.id.send_video_checkbox);
        mReceiveVideoCheckBox = view.findViewById(R.id.receive_video_checkbox);
        mSwitchCameraButton = view.findViewById(R.id.switch_camera_button);
        mAudioDeviceButton = view.findViewById(R.id.speaker_button);

        mVideoViewsHelper = new VideoViewsHelper(getContext(), videoViewsLayout);
        mPresenter.start();

        final LinearLayout moreSettingsLayout = view.findViewById(R.id.more_settings_layout);
        mMoreSettingsButton = view.findViewById(R.id.more_settings_button);
        mMoreSettingsButton.setOnClickListener(v -> {
            if (moreSettingsLayout.isShown()) {
                moreSettingsLayout.setVisibility(View.INVISIBLE);
            } else {
                moreSettingsLayout.setVisibility(View.VISIBLE);
            }
        });
        mMoreSettingsButton.setOnTouchListener((v, event) -> {
            changeButton(mMoreSettingsButton, event.getAction(), false);
            return false;
        });

        mHangupButton.setOnClickListener(v -> mPresenter.stopCall());
        mHangupButton.setOnTouchListener((v, event) -> {
            changeButton(mHangupButton, event.getAction(), true);
            return false;
        });

        mMuteAudioButton.setOnClickListener(v -> mPresenter.muteAudio());
        mMuteAudioButton.setOnTouchListener((v, event) -> {
            changeButton(mMuteAudioButton, event.getAction(), false);
            return false;
        });

        mHoldButton.setOnClickListener(v -> mPresenter.hold());
        mHoldButton.setOnTouchListener((v, event) -> {
            changeButton(mHoldButton, event.getAction(), false);
            return false;
        });

        mSwitchCameraButton.setOnClickListener(v -> mPresenter.switchCamera());
        mSwitchCameraButton.setOnTouchListener((v, event) -> {
            changeButton(mSwitchCameraButton, event.getAction(), false);
            return false;
        });

        mAudioDeviceButton.setOnClickListener(v -> showAudioDeviceSelectionDialog(mPresenter.getAudioDevices()));
        mAudioDeviceButton.setOnTouchListener((v, event) -> {
            changeButton(mAudioDeviceButton, event.getAction(), false);
            return false;
        });

        mSendVideoCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> mPresenter.sendVideo(isChecked));
        mReceiveVideoCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> mPresenter.receiveVideo());

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
        mListener = null;
    }


    @Override
    public void onHiddenChanged (boolean hidden) {
        if (hidden) {
            mVideoViewsHelper.hideAllViews();
        } else {
            mVideoViewsHelper.showAllViews();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mAlertDialog = null;
        mCallStatusTextView = null;
        mHangupButton = null;
        mMoreSettingsButton = null;
        mSwitchCameraButton = null;
        mMuteAudioButton = null;
        mHoldButton = null;
        mAudioDeviceButton = null;
    }

    private void changeButton(ImageButton button, int action, boolean isRed) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                button.setColorFilter(getResources().getColor(R.color.colorWhite));
                button.setBackground(isRed ? getResources().getDrawable(R.drawable.button_image_red_active) : getResources().getDrawable(R.drawable.button_image_active));
                break;
            case MotionEvent.ACTION_UP:
                button.setColorFilter(isRed ? getResources().getColor(R.color.colorRed) : getResources().getColor(R.color.colorAccent));
                button.setBackground(isRed ? getResources().getDrawable(R.drawable.button_image_red_passive) : getResources().getDrawable(R.drawable.button_image_passive));
                break;
        }
    }

    @Override
    public void updateAudioDeviceButton(AudioDevice audioDevice) {
        switch (audioDevice) {
            case EARPIECE:
                mAudioDeviceButton.setImageResource(R.drawable.ic_hearing_black_35dp);
                break;
            case SPEAKER:
                mAudioDeviceButton.setImageResource(R.drawable.ic_volume_up_black_35dp);
                break;
            case WIRED_HEADSET:
                mAudioDeviceButton.setImageResource(R.drawable.ic_headset_black_35dp);
                break;
            case BLUETOOTH:
                mAudioDeviceButton.setImageResource(R.drawable.ic_bluetooth_audio_black_35dp);
                break;
        }
    }

    @Override
    public void createLocalVideoView() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> mPresenter.localVideoViewCreated(mVideoViewsHelper.addLocalVideoView()));
        }
    }

    @Override
    public void removeLocalVideoView() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> mPresenter.localVideoViewRemoved(mVideoViewsHelper.removeLocalVideoView()));
        }
    }

    @Override
    public void createRemoteVideoView(String streamId, String displayName) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> mPresenter.remoteVideoViewCreated(streamId, mVideoViewsHelper.addRemoteVideoView(streamId, displayName)));
        }
    }

    @Override
    public void removeRemoteVideoView(String streamId) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> mPresenter.remoteVideoViewRemoved(streamId, mVideoViewsHelper.removeRemoteVideoView(streamId)));
        }
    }

    @Override
    public void removeAllVideoViews() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> mVideoViewsHelper.removeAllVideoViews());
        }
    }

    @Override
    public void updateRemoteVideoView(String streamId, String displayName) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> mVideoViewsHelper.updateRemoteVideoView(streamId, displayName));
        }
    }

    @Override
    public void showVideoControls(boolean show) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (show) {
                    mSwitchCameraButton.setVisibility(View.VISIBLE);
                } else {
                    mSwitchCameraButton.setVisibility(View.INVISIBLE);
                }
            });
        }
    }

    private void showAudioDeviceSelectionDialog(List<String> audioDevices) {
        if (getActivity() != null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.alert_select_audio_device)
                    .setItems(audioDevices.toArray(new String[audioDevices.size()]), (dialog, which) -> {
                        if (audioDevices.get(which).equals(AudioDevice.EARPIECE.toString())) {
                            Voximplant.getAudioDeviceManager().selectAudioDevice(AudioDevice.EARPIECE);
                        } else if (audioDevices.get(which).equals(AudioDevice.SPEAKER.toString())) {
                            Voximplant.getAudioDeviceManager().selectAudioDevice(AudioDevice.SPEAKER);
                        } else if (audioDevices.get(which).equals(AudioDevice.WIRED_HEADSET.toString())) {
                            Voximplant.getAudioDeviceManager().selectAudioDevice(AudioDevice.WIRED_HEADSET);
                        } else if (audioDevices.get(which).equals(AudioDevice.BLUETOOTH.toString())) {
                            Voximplant.getAudioDeviceManager().selectAudioDevice(AudioDevice.BLUETOOTH);
                        }
                    });
            getActivity().runOnUiThread(() -> builder.create().show());
        }
    }

    @Override
    public void updateCallStatus(int resStatus) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (resStatus != -1) {
                    mCallStatusTextView.setText(resStatus);
                } else {
                    mCallStatusTextView.setText("");
                }
            });
        }
    }

    @Override
    public void updateMicButton(boolean pressed) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> changeButton(mMuteAudioButton, pressed ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP, false));
        }
    }

    @Override
    public void updateHoldButton(boolean pressed) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> changeButton(mHoldButton, pressed ? MotionEvent.ACTION_DOWN : MotionEvent.ACTION_UP, false));
        }
    }

    @Override
    public void updateSendVideoCheckbox(boolean checked) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (mSendVideoCheckBox.isChecked() != checked) {
                    mSendVideoCheckBox.setChecked(checked);
                }
            });
        }
    }

    @Override
    public void updateReceiveVideoCheckbox(boolean checked) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (mReceiveVideoCheckBox.isChecked() != checked) {
                    mReceiveVideoCheckBox.setChecked(checked);
                }
                if (checked) {
                    mReceiveVideoCheckBox.setEnabled(false);
                }
            });
        }
    }

    @Override
    public void updateCameraButton(boolean isFront) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isFront) {
                    mSwitchCameraButton.setImageResource(R.drawable.ic_camera_rear_black_35dp);
                } else {
                    mSwitchCameraButton.setImageResource(R.drawable.ic_camera_front_black_35dp);
                }
            });
        }
    }

    @Override
    public void callDisconnected() {
        if (mListener != null) {
            mListener.onCallDisconnected(callId);
        }
    }

    @Override
    public void callFailed(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> mAlertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.call_failed)
                    .setMessage("Reason: " + error)
                    .setPositiveButton("OK", (dialog, which) -> {
                        if (mListener != null) {
                            mListener.onCallDisconnected(callId);
                        }
                    })
                    .setOnDismissListener(dialog -> {
                        if (mListener != null) {
                            mListener.onCallDisconnected(callId);
                        }
                    })
                    .show());
        }
    }

    @Override
    public void showError(int resError, String param1, String param2) {
        if (getActivity() != null) {
            String error = getString(resError, param1, param2);
            getActivity().runOnUiThread(() -> Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show());
        }
    }

    public interface OnCallFragmentListener {
        void onCallDisconnected(String callId);
    }
}
