/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.login;

import com.voximplant.sdk.client.LoginError;
import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.Shared;
import com.voximplant.sdkdemo.manager.IClientManagerListener;
import com.voximplant.sdkdemo.manager.VoxClientManager;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import java.lang.ref.WeakReference;

import static com.voximplant.sdkdemo.utils.Constants.USERNAME;

public class LoginPresenter implements LoginContract.Presenter, IClientManagerListener {
    private static final String POSTFIX = ".voximplant.com";

    private final WeakReference<LoginContract.View> mView;
    private VoxClientManager mClientManager = Shared.getInstance().getClientManager();

    LoginPresenter(LoginContract.View loginView) {
        mView = new WeakReference<>(loginView);
    }


    @Override
    public void start() {
        mClientManager.addListener(this);
        String previousUser = SharedPreferencesHelper.get().getStringFromPrefs(USERNAME);
        LoginContract.View view = mView.get();
        if (previousUser != null && view != null) {
            view.fillUsername(previousUser.replace(POSTFIX, ""));
        }
    }

    @Override
    public void loginWithPassword(String user, String password) {
        LoginContract.View view = mView.get();
        if (view == null) {
            return;
        }
        if (user == null || user.isEmpty()) {
            view.usernameInvalid(R.string.error_field_required);
            return;
        }
        if (password == null || password.isEmpty()) {
            view.passwordInvalid(R.string.error_field_required);
            return;
        }
        if (!user.contains(POSTFIX)) {
            user = user + POSTFIX;
        }
        view.showProgress(true);
        mClientManager.login(user, password);
    }

    @Override
    public void stop() {
        mClientManager.removeListener(this);
    }

    @Override
    public void onConnectionFailed() {
        LoginContract.View view = mView.get();
        if (view != null) {
            view.showProgress(false);
            view.showError(R.string.alert_content_connection_failed);
        }
    }

    @Override
    public void onLoginFailed(LoginError reason) {
        LoginContract.View view = mView.get();
        if (view == null) {
            return;
        }
        view.showProgress(false);
        switch (reason) {
            case INVALID_PASSWORD:
                view.passwordInvalid(R.string.error_incorrect_password);
                break;
            case ACCOUNT_FROZEN:
                view.showError(R.string.alert_login_failed_account_frozen);
                break;
            case INVALID_USERNAME:
                view.usernameInvalid(R.string.error_incorrect_username);
                break;
            case TIMEOUT:
                view.showError(R.string.alert_login_failed_timeout);
                break;
            case NETWORK_ISSUES:
                view.showError(R.string.alert_login_failed_network_issues);
                break;
            case INTERNAL_ERROR:
            default:
                view.showError(R.string.alert_login_failed_internal_error);
                break;
        }
    }

    @Override
    public void onLoginSuccess(String displayName) {
        LoginContract.View view = mView.get();
        if (view != null) {
            view.showProgress(false);
            view.loginSuccess(displayName);
        }
    }

}
