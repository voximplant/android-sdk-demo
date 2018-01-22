/*
 * Copyright (c) 2011-2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.voximplant.sdk.client.LoginError;
import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.SDKDemoApplication;
import com.voximplant.sdkdemo.utils.SharedPreferencesHelper;

import static com.voximplant.sdkdemo.utils.Constants.CONNECTION_FAILED;
import static com.voximplant.sdkdemo.utils.Constants.DISPLAY_NAME;
import static com.voximplant.sdkdemo.utils.Constants.ERROR_CODE;
import static com.voximplant.sdkdemo.utils.Constants.EVENT;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_FAILED;
import static com.voximplant.sdkdemo.utils.Constants.LOGIN_SUCCESSFUL;
import static com.voximplant.sdkdemo.utils.Constants.USERNAME;
import static com.voximplant.sdkdemo.utils.Constants.VOX_INTENT;

public class LoginActivity extends AppCompatActivity {

    private static final String POSTFIX = ".voximplant.com";

    private EditText mLoginView;
    private EditText mPasswordView;
    private View mProgressView;
    private Button mLoginButton;

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String event = intent.getStringExtra(EVENT);
            switch (event) {
                case LOGIN_SUCCESSFUL:
                    loginSuccessful(intent.getStringExtra(DISPLAY_NAME));
                    break;
                case LOGIN_FAILED:
                    LoginError failureReason = (LoginError)intent.getSerializableExtra(ERROR_CODE);
                    loginFailed(failureReason);
                    break;
                case CONNECTION_FAILED:
                    connectionFailed();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoginView = (EditText) findViewById(R.id.email);
        String previousUser = SharedPreferencesHelper.get().getStringFromPrefs(USERNAME);
        if (previousUser != null) {
            mLoginView.setText(previousUser.replace(POSTFIX, ""));
        }

        mPasswordView = (EditText) findViewById(R.id.password);

        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    hideKeyboard(textView);
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginButton = (Button) findViewById(R.id.email_sign_in_button);
        mLoginButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                hideKeyboard(view);
                attemptLogin();
            }
        });

        mProgressView = findViewById(R.id.login_progress);
        showProgress(false);
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(broadcastReceiver, new IntentFilter(VOX_INTENT));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(broadcastReceiver);
    }

    private void attemptLogin() {
        mLoginView.setError(null);
        mPasswordView.setError(null);

        String username;
        if (mLoginView.getText().toString().contains(POSTFIX)) {
            username = mLoginView.getText().toString();
        } else {
            username = mLoginView.getText().toString() + POSTFIX;
        }
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_short_password));
            focusView = mPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(username)) {
            mLoginView.setError(getString(R.string.error_field_required));
            focusView = mLoginView;
            cancel = true;
        } else if (!isUsernameValid(username)) {
            mLoginView.setError(getString(R.string.error_invalid_email));
            focusView = mLoginView;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            enableLoginForm(false);
            ((SDKDemoApplication) getApplication()).getClientManager().login(username, password);
        }
    }

    private boolean isUsernameValid(String username) {
        //Replace this with your own logic
        return username.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //Replace this with your own logic
        return password.length() > 4;
    }

    private void enableLoginForm(boolean enable) {
        mLoginView.setEnabled(enable);
        mPasswordView.setEnabled(enable);
        mLoginButton.setEnabled(enable);
    }

    private void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
        mProgressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }


    private void loginSuccessful(final String displayName) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.putExtra(DISPLAY_NAME, displayName);
                startActivity(intent);
                showProgress(false);
                enableLoginForm(true);
            }
        });
    }


    private void loginFailed(LoginError reason) {
        View focusView = null;
        switch (reason) {
            case INVALID_PASSWORD:
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                focusView = mPasswordView;
                break;
            case ACCOUNT_FROZEN:
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle(R.string.alert_login_failed)
                        .setMessage(R.string.alert_login_failed_account_frozen)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                break;
            case INVALID_USERNAME:
                mLoginView.setError(getString(R.string.error_incorrect_username));
                focusView = mLoginView;
                break;
            case INTERNAL_ERROR:
                new AlertDialog.Builder(LoginActivity.this)
                        .setTitle(R.string.alert_login_failed)
                        .setMessage(R.string.alert_login_failed_internal_error)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }
                        })
                        .show();
                break;
        }
        if (focusView != null) {
            focusView.requestFocus();
        }
        enableLoginForm(true);
        showProgress(false);
    }

    private void connectionFailed() {
        new AlertDialog.Builder(LoginActivity.this)
                .setTitle(R.string.alert_title_connection_failed)
                .setMessage(R.string.alert_content_connection_failed)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
        enableLoginForm(true);
        showProgress(false);
    }

    private void hideKeyboard(View view) {
        InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

}

