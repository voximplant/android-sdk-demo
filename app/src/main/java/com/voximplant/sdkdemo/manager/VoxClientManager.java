/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.manager;

import android.util.Log;

import com.voximplant.sdk.client.AuthParams;
import com.voximplant.sdk.client.ClientState;
import com.voximplant.sdk.client.IClient;
import com.voximplant.sdk.client.IClientLoginListener;
import com.voximplant.sdk.client.IClientSessionListener;
import com.voximplant.sdk.client.LoginError;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.voximplant.sdkdemo.utils.Constants.APP_TAG;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_ACCESS_EXPIRE;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_ACCESS_TOKEN;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_REFRESH_EXPIRE;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_REFRESH_TOKEN;
import static com.voximplant.sdkdemo.utils.Constants.MILLISECONDS_IN_SECOND;
import static com.voximplant.sdkdemo.utils.Constants.REFRESH_TIME;
import static com.voximplant.sdkdemo.utils.Constants.USERNAME;

public class VoxClientManager implements IClientSessionListener, IClientLoginListener {
    private IClient mClient = null;
    private CopyOnWriteArrayList<IClientManagerListener> mListeners = new CopyOnWriteArrayList<>();

    private String mUsername = null;
    private String mPassword = null;
    private ArrayList<String> mServers = new ArrayList<>();
    private String mDisplayName = null;

    public VoxClientManager() {

    }

    public void setClient(IClient client) {
        mClient = client;
        mClient.setClientLoginListener(this);
        mClient.setClientSessionListener(this);
    }

    synchronized public void addListener(IClientManagerListener listener) {
        mListeners.add(listener);
    }

    synchronized public void removeListener(IClientManagerListener listener) {
        mListeners.remove(listener);
    }

    public void login(String username, String password) {
        mUsername = username;
        mPassword = password;
        if (mClient != null) {
            if (mClient.getClientState() == ClientState.DISCONNECTED) {
                try {
                    mClient.connect(false, mServers);
                } catch (IllegalStateException e) {
                    Log.e(APP_TAG, "login: exception on connect: " + e);
                }
            }
            if (mClient.getClientState() == ClientState.CONNECTED) {
                mClient.login(username, password);
            }
        }
    }

    public void logout() {
        if (mClient != null && mClient.getClientState() == ClientState.LOGGED_IN) {
            mClient.disconnect();
        }
    }

    public ClientState getState() {
        return mClient.getClientState();
    }

    public String getDisplayName() {
        return mDisplayName;
    }

    private void loginWithToken() {
        if (mClient != null) {
            if (mClient.getClientState() == ClientState.DISCONNECTED) {
                try {
                    mClient.connect(false, mServers);
                } catch (IllegalStateException e) {
                    Log.e(APP_TAG, "loginWithToken: exception on connect: " + e);
                }
            }
            if (mClient.getClientState() == ClientState.CONNECTED) {
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
        SharedPreferencesHelper.get().saveToPrefs(LOGIN_ACCESS_EXPIRE, (long) authParams.getAccessTokenTimeExpired());
        SharedPreferencesHelper.get().saveToPrefs(LOGIN_REFRESH_TOKEN, authParams.getRefreshToken());
        SharedPreferencesHelper.get().saveToPrefs(LOGIN_REFRESH_EXPIRE, (long) authParams.getRefreshTokenTimeExpired());
    }

    private boolean isTokenExpired(long lifeTime) {
        return System.currentTimeMillis() - SharedPreferencesHelper.get().getLongFromPrefs(REFRESH_TIME) > lifeTime * MILLISECONDS_IN_SECOND;
    }

    private boolean loginTokensExist() {
        return SharedPreferencesHelper.get().getStringFromPrefs(LOGIN_ACCESS_TOKEN) != null &&
                SharedPreferencesHelper.get().getStringFromPrefs(LOGIN_REFRESH_TOKEN) != null;
    }

    @Override
    public void onConnectionEstablished() {
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
    public synchronized void onConnectionFailed(String error) {
        for (IClientManagerListener listener : mListeners) {
            listener.onConnectionFailed();
        }
    }

    @Override
    public synchronized void onConnectionClosed() {
        mPassword = null;
        mDisplayName = null;
        for (IClientManagerListener listener : mListeners) {
            listener.onConnectionClosed();
        }
    }

    @Override
    public synchronized void onLoginSuccessful(String displayName, AuthParams authParams) {
        saveAuthDetailsToSharedPreferences(authParams);
        SharedPreferencesHelper.get().saveToPrefs(USERNAME, mUsername);
        mDisplayName = displayName;
        for (IClientManagerListener listener : mListeners) {
            listener.onLoginSuccess(displayName);
        }
    }

    @Override
    public synchronized void onLoginFailed(LoginError reason) {
        for (IClientManagerListener listener : mListeners) {
            listener.onLoginFailed(reason);
        }
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
