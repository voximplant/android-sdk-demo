/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.calls;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SubMenu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.voximplant.sdk.Voximplant;
import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.ui.call.CallFragment;
import com.voximplant.sdkdemo.ui.login.LoginActivity;
import com.voximplant.sdkdemo.ui.settings.SettingsActivity;
import com.voximplant.sdkdemo.utils.ForegroundCheck;
import com.voximplant.sdkdemo.utils.FragmentTransactionHelper;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static com.voximplant.sdkdemo.utils.Constants.CALL_ANSWERED;
import static com.voximplant.sdkdemo.utils.Constants.CALL_ID;
import static com.voximplant.sdkdemo.utils.Constants.DISPLAY_NAME;
import static com.voximplant.sdkdemo.utils.Constants.INCOMING_CALL_RESULT;
import static com.voximplant.sdkdemo.utils.Constants.INTENT_PROCESSED;
import static com.voximplant.sdkdemo.utils.Constants.NEW_CALL_FRAGMENT_ID;
import static com.voximplant.sdkdemo.utils.Constants.USERNAME;
import static com.voximplant.sdkdemo.utils.Constants.WITH_VIDEO;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MainContract.View, CallFragment.OnCallFragmentListener {
    private final static int PERMISSION_NOT_REQUESTED = 1;
    private final static int PERMISSION_REQUESTED_AUDIO = 2;
    private final static int PERMISSION_REQUESTED_VIDEO = 3;


    private Menu mNavigationMenu;
    private SubMenu mNavigationSubMenu;
    private EditText mCallToView;
    private ImageButton mAudioCallButton;
    private ImageButton mVideoCallButton;
    private MainContract.Presenter mPresenter;
    private FragmentTransactionHelper mFragmentTransactionHelper = new FragmentTransactionHelper(this);
    private HashMap<String, String> mActiveCalls = new HashMap<>();
    private AlertDialog mAlertDialog;
    private ArrayList<String> mDisconnectedCallsInBackground = new ArrayList<>();

    private int mPermissionRequestedMode = PERMISSION_NOT_REQUESTED;
    private boolean mIsAudioPermissionsGranted;
    private boolean mIsVideoPermissionsGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        mNavigationMenu = navigationView.getMenu();
        mNavigationMenu.addSubMenu(R.string.active_calls);
        mNavigationSubMenu = mNavigationMenu.getItem(2).getSubMenu();

        View headerView = navigationView.getHeaderView(0);
        TextView mDisplayName = headerView.findViewById(R.id.displayName);

        TextView mUserName = headerView.findViewById(R.id.userName);
        mUserName.setText(SharedPreferencesHelper.get().getStringFromPrefs(USERNAME));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDisplayName.setText(extras.getString(DISPLAY_NAME));
        }

        mCallToView = findViewById(R.id.call_to);
        mAudioCallButton = findViewById(R.id.button_audio_call);
        mVideoCallButton = findViewById(R.id.button_video_call);

        mAudioCallButton.setOnClickListener(v -> {
            mPresenter.makeCall(mCallToView.getText().toString(), false);
            hideKeyboard(v);
        });
        mAudioCallButton.setOnTouchListener((v, event) -> {
            changeButton(mAudioCallButton, event.getAction());
            return false;
        });

        mVideoCallButton.setOnClickListener(v -> {
            mPresenter.makeCall(mCallToView.getText().toString(), true);
            hideKeyboard(v);
        });
        mVideoCallButton.setOnTouchListener((v, event) -> {
            changeButton(mVideoCallButton, event.getAction());
            return false;
        });

        mPresenter = new MainPresenter(this);
        mPresenter.start();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (mAlertDialog != null) {
            mAlertDialog.dismiss();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        Intent intent = getIntent();
        if (!intent.getBooleanExtra(INTENT_PROCESSED, false)) {
            intent.putExtra(INTENT_PROCESSED, true);
            String callId = intent.getStringExtra(CALL_ID);
            int result = intent.getIntExtra(INCOMING_CALL_RESULT, -1);
            switch (result) {
                case CALL_ANSWERED:
                    boolean withVideo = intent.getBooleanExtra(WITH_VIDEO, false);
                    mPresenter.answerCall(callId, withVideo);
                    break;
            }
        }

        if (!mDisconnectedCallsInBackground.isEmpty()) {
            for (String call_id : mDisconnectedCallsInBackground) {
                removeCallFragment(call_id);
            }
            mDisconnectedCallsInBackground.clear();
        }

        if (mPermissionRequestedMode == PERMISSION_REQUESTED_VIDEO && mIsAudioPermissionsGranted && mIsVideoPermissionsGranted) {
            mPresenter.permissionsAreGrantedForCall();
        } else if (mPermissionRequestedMode == PERMISSION_REQUESTED_AUDIO && mIsAudioPermissionsGranted) {
            mPresenter.permissionsAreGrantedForCall();
        }
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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String title = item.getTitle().toString();

        if (id == R.id.nav_logout) {
            item.setChecked(false);
            showAlertDialog(R.string.alert_title_logout, R.string.alert_content_logout);
        }
        if (id == R.id.nav_setting) {
            item.setChecked(false);
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }

        if (title.equals(getString(R.string.make_new_call))) {
            mFragmentTransactionHelper.findAndHideVisibleFragment();
            enableNewCallControls(true);
            item.setChecked(true);
        } else if (mActiveCalls.containsValue(title)) {
            for (Map.Entry<String, String> entry : mActiveCalls.entrySet()) {
                if (entry.getValue().equals(title)) {
                    item.setChecked(true);
                    enableNewCallControls(false);
                    mFragmentTransactionHelper.findAndHideVisibleFragment();
                    mFragmentTransactionHelper.showFragment(entry.getKey());
                }
            }
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void changeButton(ImageButton button, int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                button.setColorFilter(getResources().getColor(R.color.colorWhite));
                button.setBackground(getResources().getDrawable(R.drawable.button_image_active));
                break;
            case MotionEvent.ACTION_UP:
                button.setColorFilter(getResources().getColor(R.color.colorAccent));
                button.setBackground(getResources().getDrawable(R.drawable.button_image_passive));
                break;
        }
    }

    @Override
    public void notifyConnectionClosed() {
        showAlertDialog(R.string.alert_title_disconnected, R.string.alert_content_disconnected);
    }

    @Override
    public void notifyInvalidCallUser() {
        mCallToView.setError(getString(R.string.error_field_required));
        mCallToView.requestFocus();
    }

    @Override
    public void startCallFragment(String callId, boolean withVideo, String user, boolean isIncoming) {
        mActiveCalls.put(callId, user);
        addCallToNavigationMenu(callId, user);
        CallFragment callFragment = CallFragment.newInstance(callId, isIncoming, withVideo);
        mFragmentTransactionHelper.addFragment(callFragment, callId, R.id.callsContainer);
        enableNewCallControls(false);
    }

    @Override
    public void removeCallFragment(String callId) {
        if (ForegroundCheck.get().isForeground()) {
            mActiveCalls.remove(callId);
            removeCallFromNavigationMenu(callId);
            enableNewCallControls(mActiveCalls.isEmpty());
            mFragmentTransactionHelper.removeFragment(callId);
            mFragmentTransactionHelper.showActiveCallFragment();
        } else {
            mDisconnectedCallsInBackground.add(callId);
        }
    }

    private void showAlertDialog(int resTitle, int resContent) {
        runOnUiThread(() -> {
            mAlertDialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(resTitle)
                    .setMessage(resContent)
                    .setPositiveButton(R.string.alert_positive_button, (dialog, which) -> {
                        mPresenter.logout();
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivity(intent);
                        finish();
                    })
                    .show();
        });
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void addCallToNavigationMenu(String callId, String callUser) {
        runOnUiThread(() -> {
            if (mNavigationSubMenu.findItem(NEW_CALL_FRAGMENT_ID) == null) {
                mNavigationSubMenu.add(Menu.NONE, NEW_CALL_FRAGMENT_ID, Menu.NONE, getString(R.string.make_new_call));
            }
            mNavigationSubMenu.add(Menu.NONE, callId.hashCode(), Menu.NONE, callUser);
        });
    }

    private void removeCallFromNavigationMenu(String callId) {
        runOnUiThread(() -> {
            mNavigationSubMenu.removeItem(callId.hashCode());
            if (mActiveCalls.isEmpty()) {
                mNavigationSubMenu.removeItem(NEW_CALL_FRAGMENT_ID);
            }
        });
    }

    private void enableNewCallControls(boolean enable) {
        runOnUiThread(() -> {
            mCallToView.setEnabled(enable);
            mAudioCallButton.setEnabled(enable);
            mVideoCallButton.setEnabled(enable);
        });
    }

    @Override
    public boolean checkPermissionsGrantedForCall(boolean isVideoCall) {
        ArrayList<String> missingPermissions = (ArrayList<String>) Voximplant.getMissingPermissions(getApplicationContext(), isVideoCall);
        if (missingPermissions.size() == 0) {
            return true;
        } else {
            ActivityCompat.requestPermissions(this, missingPermissions.toArray(new String[missingPermissions.size()]), PERMISSION_GRANTED);
            mPermissionRequestedMode = isVideoCall ? PERMISSION_REQUESTED_VIDEO : PERMISSION_REQUESTED_AUDIO;
            return false;
        }
    }

    @Override
    public void onCallDisconnected(String callId) {
        removeCallFragment(callId);
    }
}
