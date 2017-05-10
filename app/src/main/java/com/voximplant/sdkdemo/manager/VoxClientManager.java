/*
 * Copyright (c) 2017, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.manager;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.google.firebase.iid.FirebaseInstanceId;
import com.voximplant.sdk.client.AuthParams;
import com.voximplant.sdk.client.IClient;
import com.voximplant.sdk.client.IClientLoginListener;
import com.voximplant.sdk.client.IClientSessionListener;
import com.voximplant.sdk.client.LoginError;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.Map;

import static com.voximplant.sdkdemo.utils.Constants.*;

public class VoxClientManager implements IClientSessionListener, IClientLoginListener {

    public enum State {
        DISCONNECTED,
        CONNECTED,
        LOGGEDIN
    }

    private IClient mClient = null;
    private boolean mIsInitialized = false;

    private State mCurrentState;
    private String mUsername = null;
    private String mPassword = null;
    private String mFireBaseToken;
    private LocalBroadcastManager mBroadcastManager;
    private ArrayList<String> mServers = new ArrayList<>();

    public VoxClientManager(IClient client, Context appContext) {
        if (!mIsInitialized) {
            mClient = client;
            mClient.setClientLoginListener(this);
            mClient.setClientSessionListener(this);
            mCurrentState = State.DISCONNECTED;
            mBroadcastManager = LocalBroadcastManager.getInstance(appContext);
            mIsInitialized = true;
            mFireBaseToken = FirebaseInstanceId.getInstance().getToken();
            //mServers.add("");
        }
    }

    public State getCurrentState() {
        return mCurrentState;
    }

    public void login(String username, String password) {
        if (mIsInitialized) {
            mUsername = username;
            mPassword = password;
            if (mClient != null) {
                if (mCurrentState == State.DISCONNECTED) {
                    mClient.connect(true, mServers);
                }
                if (mCurrentState == State.CONNECTED) {
                    mClient.login(username, password);
                }
            }
        }
    }

    public void logout() {
        if (mCurrentState == State.LOGGEDIN && mClient != null) {
            enablePushNotifications(false);
            mClient.disconnect();
            mCurrentState = State.DISCONNECTED;
        }
    }

    private void loginWithToken() {
        if (mClient != null) {
            if (mCurrentState == State.DISCONNECTED) {
                mClient.connect(false, mServers);
            }
            if (mCurrentState == State.CONNECTED) {
                if (loginTokensExist()) {
                    mUsername = SharedPreferencesHelper.get().getStringFromPrefs(USERNAME);
                    if (!isTokenExpired(SharedPreferencesHelper.get().getLongFromPrefs(LOGIN_ACCESS_EXPIRE))) {
                        mClient.loginWithAccessToken(mUsername,
                                SharedPreferencesHelper.get().getStringFromPrefs(LOGIN_ACCESS_TOKEN));
                    } else if (!isTokenExpired(SharedPreferencesHelper.get().getLongFromPrefs(LOGIN_REFRESH_EXPIRE))) {
                        mClient.refreshToken(mUsername,
                                SharedPreferencesHelper.get().getStringFromPrefs(LOGIN_REFRESH_TOKEN));
                    }
                }
            }
        }
    }

    private void saveAuthDetailsToSharedPreferences(AuthParams authParams) {
        SharedPreferencesHelper.get().saveToPrefs(REFRESH_TIME, System.currentTimeMillis());
        SharedPreferencesHelper.get().saveToPrefs(LOGIN_ACCESS_TOKEN, authParams.getAccessToken());
        SharedPreferencesHelper.get().saveToPrefs(LOGIN_ACCESS_EXPIRE, (long)authParams.getAccessTokenTimeExpired());
        SharedPreferencesHelper.get().saveToPrefs(LOGIN_REFRESH_TOKEN, authParams.getRefreshToken());
        SharedPreferencesHelper.get().saveToPrefs(LOGIN_REFRESH_EXPIRE, (long)authParams.getRefreshTokenTimeExpired());
    }

    private boolean isTokenExpired(long lifeTime) {
        return System.currentTimeMillis() - SharedPreferencesHelper.get().getLongFromPrefs(REFRESH_TIME) > lifeTime * MILLISECONDS_IN_SECOND;
    }

    private boolean loginTokensExist() {
        return SharedPreferencesHelper.get().getStringFromPrefs(LOGIN_ACCESS_TOKEN) != null &&
                SharedPreferencesHelper.get().getStringFromPrefs(LOGIN_REFRESH_TOKEN) != null;
    }

    public void firebaseTokenRefreshed(String token) {
        if (mIsInitialized) {
            mFireBaseToken = token;
            enablePushNotifications(true);
        }
    }

    public void pushNotificationReceived(Map<String, String> message) {
        if (mIsInitialized) {
            mClient.handlePushNotification(message);
            loginWithToken();
        }
    }

    public void enablePushNotifications(boolean enable) {
        if (enable) {
            mClient.registerForPushNotifications(mFireBaseToken);
        } else {
            mClient.unregisterFromPushNotifications(mFireBaseToken);
        }
    }


    @Override
    public void onConnectionEstablished() {
        mCurrentState = State.CONNECTED;
        if (mClient == null) {
            return;
        }
        if (mUsername != null && mPassword != null) {
            mClient.login(mUsername, mPassword);
        } else {
            loginWithToken();
        }
    }

    @Override
    public void onConnectionFailed(String error) {
        mCurrentState = State.DISCONNECTED;
        Intent connectionFailedIntent = new Intent(VOX_INTENT);
        connectionFailedIntent.putExtra(EVENT, CONNECTION_FAILED);
        mBroadcastManager.sendBroadcast(connectionFailedIntent);
    }

    @Override
    public void onConnectionClosed() {
        mCurrentState = State.DISCONNECTED;
        Intent disconnectedIntent = new Intent(VOX_INTENT);
        disconnectedIntent.putExtra(EVENT, DISCONNECTED);
        mBroadcastManager.sendBroadcast(disconnectedIntent);
    }

    @Override
    public void onLoginSuccessful(String displayName, AuthParams authParams) {
        mCurrentState = State.LOGGEDIN;
        enablePushNotifications(SharedPreferencesHelper.get().getBooleanFromPrefs(KEY_PREF_PUSH_ENABLE));
        saveAuthDetailsToSharedPreferences(authParams);
        SharedPreferencesHelper.get().saveToPrefs(USERNAME, mUsername);
        Intent loginSuccessfulIntent = new Intent(VOX_INTENT);
        loginSuccessfulIntent.putExtra(EVENT, LOGIN_SUCCESSFUL);
        loginSuccessfulIntent.putExtra(DISPLAY_NAME, displayName);
        mBroadcastManager.sendBroadcast(loginSuccessfulIntent);
    }

    @Override
    public void onLoginFailed(LoginError reason) {
        mCurrentState = State.CONNECTED;
        Intent loginFailedIntent = new Intent(VOX_INTENT);
        loginFailedIntent.putExtra(EVENT, LOGIN_FAILED);
        loginFailedIntent.putExtra(ERROR_CODE, reason);
        mBroadcastManager.sendBroadcast(loginFailedIntent);
    }

    @Override
    public void onOneTimeKeyGenerated(String key) {

    }

    @Override
    public void onRefreshTokenFailed(LoginError reason) {

    }

    @Override
    public void onRefreshTokenSuccess(AuthParams authParams) {
        saveAuthDetailsToSharedPreferences(authParams);
        loginWithToken();
    }

}
