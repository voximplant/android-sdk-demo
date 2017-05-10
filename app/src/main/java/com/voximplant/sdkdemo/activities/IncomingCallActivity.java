package com.voximplant.sdkdemo.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;

import com.voximplant.sdk.Voximplant;
import com.voximplant.sdk.call.CallException;
import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.call.ICallListener;
import com.voximplant.sdk.call.IVideoStream;
import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.SDKDemoApplication;
import com.voximplant.sdkdemo.manager.VoxCallManager;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.Map;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static com.voximplant.sdkdemo.utils.Constants.CALL_ANSWERED;
import static com.voximplant.sdkdemo.utils.Constants.CALL_DISCONNECTED;
import static com.voximplant.sdkdemo.utils.Constants.CALL_ID;
import static com.voximplant.sdkdemo.utils.Constants.INCOMING_CALL_RESULT;

public class IncomingCallActivity extends AppCompatActivity implements ICallListener {

    private String mCallId;
    private ICall mCall;
    private VoxCallManager mCallManager;
    private boolean mIsAudioPermissionsGranted;
    private boolean mIsVideoPermissionsGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        setContentView(R.layout.activity_incoming_call);

        if (SharedPreferencesHelper.get().getBooleanFromPrefs(getString(R.string.pref_call_vibrate_enable_key))) {
            Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(500);
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mCallId = extras.getString(CALL_ID);
            mCallManager = ((SDKDemoApplication) getApplication()).getCallManager();
            mCall = mCallManager.getCallById(mCallId);
            mCall.addCallListener(this);
        }

        TextView callFrom = (TextView) findViewById(R.id.incoming_call_from);
        if (mCall.getEndpoints().size() > 0) {
            callFrom.setText(mCall.getEndpoints().get(0).getUserDisplayName());
        }

        ImageButton answerCallButton = (ImageButton) findViewById(R.id.answer_call_button);
        final IncomingCallActivity self = this;
        answerCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionsGrantedForCall(mCall.isVideoEnabled())) {
                    answerCall();
                }
            }
        });

        ImageButton declineCallButton = (ImageButton) findViewById(R.id.decline_call_button);
        declineCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (mCall != null) {
                        mCall.reject(null);
                        mCall.removeCallListener(self);
                    }
                } catch (CallException e) {
                    Log.e("VoxImplantSDKDemo", "exception on reject call ", e);
                }
                mCallManager.removeCall(mCall.getCallId());
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mCall.isVideoEnabled() && mIsAudioPermissionsGranted && mIsVideoPermissionsGranted) {
            answerCall();
        } else if (mIsAudioPermissionsGranted) {
            answerCall();
        }
    }

    @Override
    public void onBackPressed() {
        if (mCall != null) {
            try {
                mCall.reject(null);
                mCall.removeCallListener(this);
            } catch (CallException e) {
                Log.e("VoxImplantSDKDemo", "exception on reject call ", e);
            }
            mCallManager.removeCall(mCall.getCallId());
        }
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.RECORD_AUDIO) && grantResults[i] == PERMISSION_GRANTED) {
                    mIsAudioPermissionsGranted = true;
                }
                if (permissions[i].equals(Manifest.permission.CAMERA) && grantResults[i] == PERMISSION_GRANTED) {
                    mIsVideoPermissionsGranted = true;
                }
            }
        }
    }

    private boolean permissionsGrantedForCall(boolean isVideoCall) {
        ArrayList<String> missingPermissions = (ArrayList<String>) Voximplant.getMissingPermissions(getApplicationContext(), isVideoCall);
        if (missingPermissions.size() == 0) {
            mIsAudioPermissionsGranted = true;
            mIsVideoPermissionsGranted = true;
            return true;
        } else {
            mIsAudioPermissionsGranted = !missingPermissions.contains(android.Manifest.permission.RECORD_AUDIO);
            mIsVideoPermissionsGranted = !missingPermissions.contains(android.Manifest.permission.CAMERA);
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[missingPermissions.size()]), PERMISSION_GRANTED);
            return false;
        }
    }

    private void answerCall() {
        if (mCall != null) {
            mCall.removeCallListener(this);
        }
        Intent data = new Intent(getApplicationContext(), MainActivity.class);
        data.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        data.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        data.putExtra(INCOMING_CALL_RESULT, CALL_ANSWERED);
        data.putExtra(CALL_ID, mCallId);
        startActivity(data);
        finish();
    }

    @Override
    public void onCallConnected(ICall call, Map<String, String> headers) {

    }

    @Override
    public void onCallDisconnected(ICall call, Map<String, String> headers, boolean answeredElsewhere) {
        mCall.removeCallListener(this);
        Intent data = new Intent(getApplicationContext(), MainActivity.class);
        data.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        data.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        data.putExtra(INCOMING_CALL_RESULT, CALL_DISCONNECTED);
        data.putExtra(CALL_ID, mCallId);
        startActivity(data);
        finish();
    }

    @Override
    public void onCallRinging(ICall call, Map<String, String> headers) {

    }

    @Override
    public void onCallFailed(ICall call, int code, String description, Map<String, String> headers) {

    }

    @Override
    public void onCallAudioStarted(ICall call) {

    }

    @Override
    public void onSIPInfoReceived(ICall call, String type, String content, Map<String, String> headers) {

    }

    @Override
    public void onMessageReceived(ICall call, String text) {

    }

    @Override
    public void onLocalVideoStreamAdded(ICall call, IVideoStream videoStream) {

    }

    @Override
    public void onLocalVideoStreamRemoved(ICall call, IVideoStream videoStream) {

    }
}
