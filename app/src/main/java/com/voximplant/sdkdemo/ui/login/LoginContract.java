/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.login;

import com.voximplant.sdkdemo.BasePresenter;
import com.voximplant.sdkdemo.BaseView;

public interface LoginContract {
    interface View extends BaseView<Presenter> {
        void fillUsername(String username);
        void showProgress(boolean show);
        void loginSuccess(String displayName);
        void usernameInvalid(int error);
        void passwordInvalid(int error);
        void showError(int error);
    }

    interface Presenter extends BasePresenter {
        void loginWithPassword(String user, String password);
        void stop();
    }
}
