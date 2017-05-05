/*
 * Copyright (c) 2017, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.activities;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.view.SubMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.voximplant.sdk.call.ICall;
import com.voximplant.sdk.Voximplant;
import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.SDKDemoApplication;
import com.voximplant.sdkdemo.fragments.CallFragment;
import com.voximplant.sdkdemo.manager.VoxCallManager;
import com.voximplant.sdkdemo.manager.VoxClientManager;
import com.voximplant.sdkdemo.utils.ForegroundCheck;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.support.v4.content.PermissionChecker.PERMISSION_GRANTED;
import static com.voximplant.sdkdemo.utils.Constants.*;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener,
        CallFragment.OnCallFragmentListener {

    private ArrayList<String> mRecentCalls = new ArrayList<>();
    private ArrayAdapter<String> mRecentCallsListAdapter;
    private EditText mCallToView;

    private boolean isVisible;
    private String mDisconnectCallId;

    private VoxCallManager mCallManager;
    private SubMenu mNavigationSubMenu;
    private boolean isNewVideoCall;
    private String mCallTo;
    private HashMap<String, String> callUsersList = new HashMap<>();
    private boolean mIsAudioPermissionsGranted;
    private boolean mIsVideoPermissionsGranted;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra(EVENT);
            switch (event) {
                case DISCONNECTED:
                    showAlertDialog(getString(R.string.alert_title_disconnected),
                            getString(R.string.alert_content_disconnected));
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        mCallManager = ((SDKDemoApplication) getApplication()).getCallManager();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        Menu mNavigationMenu = mNavigationView.getMenu();
        mNavigationMenu.addSubMenu(R.string.active_calls);
        mNavigationSubMenu = mNavigationMenu.getItem(2).getSubMenu();

        View headerView = mNavigationView.getHeaderView(0);
        TextView mDisplayName = (TextView) headerView.findViewById(R.id.displayName);

        TextView mUserName = (TextView) headerView.findViewById(R.id.userName);
        mUserName.setText(SharedPreferencesHelper.get().getStringFromPrefs(USERNAME));

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mDisplayName.setText(extras.getString(DISPLAY_NAME));
        }

        mCallToView = (EditText) findViewById(R.id.call_to);

        TextView recentCallsListHeader = new TextView(this);
        recentCallsListHeader.setText(R.string.recent_calls);
        recentCallsListHeader.setTextSize(16);
        ListView recentCallsListView = (ListView) findViewById(R.id.recent_calls_list);
        recentCallsListView.addHeaderView(recentCallsListHeader);
        recentCallsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!((TextView)view).getText().toString().equals(getString(R.string.recent_calls))) {
                    mCallToView.setText(((TextView)view).getText().toString());
                }
            }
        });

        mRecentCallsListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, mRecentCalls);
        recentCallsListView.setAdapter(mRecentCallsListAdapter);

        ImageButton audioCallButton = (ImageButton) findViewById(R.id.button_audio_call);
        audioCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeCall(mCallToView.getText().toString(), false);
                hideKeyboard(v);
            }
        });
        ImageButton videoCallButton = (ImageButton) findViewById(R.id.button_video_call);
        videoCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                makeCall(mCallToView.getText().toString(), true);
                hideKeyboard(v);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mBroadcastReceiver, new IntentFilter(VOX_INTENT));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

    }

    @Override
    public void onResume() {
        super.onResume();
        isVisible = true;
        if (((SDKDemoApplication) getApplication()).getClientManager().getCurrentState() == VoxClientManager.State.DISCONNECTED) {
            showAlertDialog(getString(R.string.alert_title_disconnected),
                    getString(R.string.alert_content_disconnected));
        } else {
            Intent intent = getIntent();
            if (!intent.getBooleanExtra("processed", false)) {
                intent.putExtra("processed", true);

                String callId = intent.getStringExtra(CALL_ID);
                int result = intent.getIntExtra(INCOMING_CALL_RESULT, -1);
                switch (result) {
                    case CALL_ANSWERED:
                        onAnswerCall(callId);
                        break;
                    case CALL_DISCONNECTED:
                        onCallDisconnected(callId);
                        break;
                }
            }

            if (mCallTo != null) {
                if (isNewVideoCall && mIsAudioPermissionsGranted && mIsVideoPermissionsGranted) {
                    makeCall(mCallTo, true);
                    isNewVideoCall = false;
                } else if (mIsAudioPermissionsGranted) {
                    makeCall(mCallTo, false);
                }
            }
            if (mDisconnectCallId != null) {
                onCallDisconnected(mDisconnectCallId);
            }
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

    @Override
    public void onPause() {
        super.onPause();
        isVisible = false;
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        String title = item.getTitle().toString();

        if (id == R.id.nav_logout) {
            logoutAndFinish();
        }
        if (id == R.id.nav_setting) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        }
        if (title.equals(getString(R.string.make_new_call))) {
            findAndHideVisibleFragment();
        } else {
            for (Map.Entry<String, String> entry : callUsersList.entrySet()) {
                if (entry.getValue().equals(title)) {
                    Fragment callFragment = getSupportFragmentManager().findFragmentByTag(entry.getKey());
                    if (callFragment != null && callFragment.isHidden()) {
                        findAndHideVisibleFragment();
                        showFragment(getSupportFragmentManager().findFragmentByTag(entry.getKey()));
                    }
                }
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void endAllCallsIfAny() {
        for (Map.Entry<String, String> entry : callUsersList.entrySet()) {
            CallFragment callFragment = (CallFragment) getSupportFragmentManager().findFragmentByTag(entry.getKey());
            if (callFragment != null) {
                callFragment.endCall();
            }
        }
    }

    private void logoutAndFinish() {
        if (callUsersList.isEmpty()) {
            showAlertDialog(getString(R.string.alert_title_logout),
                    getString(R.string.alert_content_logout));
        } else {
            showAlertDialog(getString(R.string.alert_title_logout),
                    getString(R.string.alert_content_logout_with_active_calls));
        }
    }

    private void showAlertDialog(String title, String text_content) {
        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(text_content)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mBroadcastReceiver);
                        endAllCallsIfAny();
                        ((SDKDemoApplication) getApplication()).getClientManager().logout();
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private void makeCall(String to, boolean isVideoCall) {

        if (mCallToView.getText().toString().isEmpty()) {
            mCallToView.setError(getString(R.string.error_field_required));
            mCallToView.requestFocus();
            return;
        }
        mCallToView.setError(null);
        updateRecentCallsList(mCallToView.getText().toString());
        mCallToView.setText("");

        mCallTo = to;
        if (permissionsGrantedForCall(isVideoCall)) {
            ICall newCall = mCallManager.createCall(to, isVideoCall);
            if (newCall != null) {
                startCallFragment(newCall, false);
                mCallTo = null;
                isNewVideoCall = false;
            }
        } else {
            isNewVideoCall = isVideoCall;
        }
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void onAnswerCall(String callId) {
        updateRecentCallsList(mCallManager.getCallById(callId).getEndpoints().get(0).getUserName());
        startCallFragment(mCallManager.getCallById(callId), true);
        mCallManager.removeCall(callId);
    }

    @Override
    public void onCallDisconnected(final String callId) {
        if (ForegroundCheck.get().isForeground() && isVisible) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (callUsersList.containsKey(callId)) {
                        callUsersList.remove(callId);
                        removeCallFromNavigationMenu(callId);
                    }
                }
            });
            removeFragment(getSupportFragmentManager().findFragmentByTag(callId));
            showActiveCallFragment();
            mCallManager.removeCall(callId);
            mDisconnectCallId = null;
        } else {
            mCallManager.removeCall(callId);
            mDisconnectCallId = callId;
        }
    }

    private void addCallToNavigationMenu(String callId, String callUser) {
        if (mNavigationSubMenu.findItem(NEW_CALL_FRAGMENT_ID) == null) {
            mNavigationSubMenu.add(Menu.NONE, NEW_CALL_FRAGMENT_ID, Menu.NONE, getString(R.string.make_new_call));
        }
        mNavigationSubMenu.add(Menu.NONE, callId.hashCode(), Menu.NONE, callUser);
    }

    private void removeCallFromNavigationMenu(String callId) {
        mNavigationSubMenu.removeItem(callId.hashCode());
        if (callUsersList.isEmpty()) {
            mNavigationSubMenu.removeItem(NEW_CALL_FRAGMENT_ID);
        }
    }

    private void updateRecentCallsList(String userName) {
        mRecentCalls.add(userName);
        mRecentCallsListAdapter.notifyDataSetChanged();
    }

    private void startCallFragment(ICall call, boolean isIncomingCall) {
        String callUser;
        if (isIncomingCall) {
            callUser = call.getEndpoints().get(0).getUserName();
        } else {
            callUser = mCallTo;
        }
        callUsersList.put(call.getCallId(), callUser);
        addCallToNavigationMenu(call.getCallId(), callUser);
        findAndHideVisibleFragment();
        CallFragment callFragment = CallFragment.newInstance(call.getCallId(), isIncomingCall);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.callsContainer, callFragment, call.getCallId())
                .commit();
        callFragment.setCall(call);
    }

    private void showFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().show(fragment).commit();
        }
    }

    private void hideFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager().beginTransaction().hide(fragment).commit();
        }
    }

    private void removeFragment(Fragment fragment) {
        if(fragment != null) {
            getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }

    private void findAndHideVisibleFragment() {
        for (Map.Entry<String, String> entry : callUsersList.entrySet()) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(entry.getKey());
            if (fragment != null && fragment.isVisible()) {
                hideFragment(fragment);
            }
        }
    }

    private void showActiveCallFragment() {
        for (Map.Entry<String, String> entry : callUsersList.entrySet()) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(entry.getKey());
            if (fragment != null && fragment.isHidden()) {
                showFragment(fragment);
            }
        }
    }
}
