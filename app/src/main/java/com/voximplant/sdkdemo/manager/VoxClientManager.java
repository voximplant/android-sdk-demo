/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.manager;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.voximplant.sdk.Voximplant;
import com.voximplant.sdk.client.AuthParams;
import com.voximplant.sdk.client.ClientState;
import com.voximplant.sdk.client.IClient;
import com.voximplant.sdk.client.IClientLoginListener;
import com.voximplant.sdk.client.IClientSessionListener;
import com.voximplant.sdk.client.LoginError;
import com.voximplant.sdk.messaging.IMessenger;
import com.voximplant.sdk.messaging.MessengerNotifications;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.voximplant.sdkdemo.utils.Constants.APP_TAG;
import static com.voximplant.sdkdemo.utils.Constants.KEY_PREF_PUSH_ENABLE;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_ACCESS_EXPIRE;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_ACCESS_TOKEN;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_REFRESH_EXPIRE;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_REFRESH_TOKEN;
import static com.voximplant.sdkdemo.utils.Constants.MILLISECONDS_IN_SECOND;
import static com.voximplant.sdkdemo.utils.Constants.REFRESH_TIME;
import static com.voximplant.sdkdemo.utils.Constants.USERNAME;

public class VoxClientManager implements IClientSessionListener, IClientLoginListener {

    public enum State {
        DISCONNECTED,
        CONNECTED,
        LOGGEDIN
    }

    private IClient mClient = null;
    private CopyOnWriteArrayList<IClientManagerListener> mListeners = new CopyOnWriteArrayList<>();

    private State mCurrentState;
    private String mUsername = null;
    private String mPassword = null;
    private String mFireBaseToken;
    private ArrayList<String> mServers = new ArrayList<>();

    public VoxClientManager() {
        mCurrentState = State.DISCONNECTED;
        mFireBaseToken = FirebaseInstanceId.getInstance().getToken();
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

    public State getCurrentState() {
        return mCurrentState;
    }

    public void login(String username, String password) {
        mUsername = username;
        mPassword = password;
        if (mClient != null) {
            if (mCurrentState == State.DISCONNECTED) {
                try {
                    mClient.connect(false, mServers);
                } catch (IllegalStateException e) {
                    Log.e(APP_TAG, "login: exception on connect: " + e);
                }
            }
            if (mCurrentState == State.CONNECTED) {
                mClient.login(username, password);
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
                try {
                    mClient.connect(false, mServers);
                } catch (IllegalStateException e) {
                    Log.e(APP_TAG, "loginWithToken: exception on connect: " + e);
                }
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

    public void firebaseTokenRefreshed(String token) {
        mFireBaseToken = token;
        enablePushNotifications(true);
    }

    public void pushNotificationReceived(Map<String, String> message) {
        mClient.handlePushNotification(message);
        loginWithToken();
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
    public synchronized void onConnectionFailed(String error) {
        mCurrentState = State.DISCONNECTED;
        for (IClientManagerListener listener : mListeners) {
            listener.onConnectionFailed();
        }
    }

    @Override
    public synchronized void onConnectionClosed() {
        mCurrentState = State.DISCONNECTED;
        for (IClientManagerListener listener : mListeners) {
            listener.onConnectionClosed();
        }
    }

    @Override
    public synchronized void onLoginSuccessful(String displayName, AuthParams authParams) {
        mCurrentState = State.LOGGEDIN;
        enablePushNotifications(SharedPreferencesHelper.get().getBooleanFromPrefs(KEY_PREF_PUSH_ENABLE));
        saveAuthDetailsToSharedPreferences(authParams);
        SharedPreferencesHelper.get().saveToPrefs(USERNAME, mUsername);
        for (IClientManagerListener listener : mListeners) {
            listener.onLoginSuccess(displayName);
        }

        IMessenger messenger = Voximplant.getMessenger();
        List<MessengerNotifications> notifications = new ArrayList<>();
        notifications.add(MessengerNotifications.ON_EDIT_MESSAGE);
        notifications.add(MessengerNotifications.ON_SEND_MESSAGE);
        if (messenger != null) {
            messenger.managePushNotifications(notifications);
        }
    }

    @Override
    public synchronized void onLoginFailed(LoginError reason) {
        if (reason == LoginError.TIMEOUT) {
            mCurrentState = State.CONNECTED;
        } else if (reason == LoginError.INVALID_STATE && mClient.getClientState() == ClientState.CONNECTED) {
            mCurrentState = State.CONNECTED;
        } else if (reason == LoginError.NETWORK_ISSUES) {
            mCurrentState = State.DISCONNECTED;
        } else {
            mCurrentState = State.CONNECTED;
        }

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
