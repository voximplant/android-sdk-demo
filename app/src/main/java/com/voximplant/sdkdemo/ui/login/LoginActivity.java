/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.voximplant.sdkdemo.R;
import com.voximplant.sdkdemo.ui.calls.MainActivity;

import static com.voximplant.sdkdemo.utils.Constants.DISPLAY_NAME;

public class LoginActivity extends AppCompatActivity implements LoginContract.View {
    private LoginContract.Presenter mLoginPresenter;

    private EditText mLoginView;
    private EditText mPasswordView;
    private View mProgressView;
    private Button mLoginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mLoginPresenter = new LoginPresenter(this);

        mLoginView = findViewById(R.id.email);
        mPasswordView = findViewById(R.id.password);
        mProgressView = findViewById(R.id.login_progress);
        mLoginButton = findViewById(R.id.email_sign_in_button);

        mPasswordView.setOnEditorActionListener((textView, i, keyEvent) -> {
            if (i == 100 || i == EditorInfo.IME_NULL) {
                hideKeyboard(textView);
                mLoginPresenter.loginWithPassword(mLoginView.getText().toString(), mPasswordView.getText().toString());
                return true;
            }
            return false;
        });

        mLoginButton.setOnClickListener(view -> {
            hideKeyboard(view);
            mLoginPresenter.loginWithPassword(mLoginView.getText().toString(), mPasswordView.getText().toString());
        });

        mLoginPresenter = new LoginPresenter(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        mLoginPresenter.start();
    }

    public void hideKeyboard(View view) {
        InputMethodManager imm =  (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    public void fillUsername(String username) {
        runOnUiThread(() -> mLoginView.setText(username));
    }

    @Override
    public void showProgress(boolean show) {
        runOnUiThread(() -> {
            mLoginView.setEnabled(!show);
            mPasswordView.setEnabled(!show);
            mLoginButton.setEnabled(!show);

            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });
        });
    }

    @Override
    public void loginSuccess(String displayName) {
        mLoginPresenter.stop();
        runOnUiThread(() -> {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.putExtra(DISPLAY_NAME, displayName);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void usernameInvalid(int error) {
        runOnUiThread(() -> {
            mLoginView.setError(getString(error));
            mLoginView.requestFocus();
        });
    }

    @Override
    public void passwordInvalid(int error) {
        runOnUiThread(() -> {
            mPasswordView.setError(getString(error));
            mPasswordView.requestFocus();
        });
    }

    @Override
    public void showError(int error) {
        runOnUiThread(() -> {
            int titleRes;
            switch (error) {
                case R.string.alert_content_connection_failed:
                    titleRes = R.string.alert_title_connection_failed;
                    break;
                default:
                    titleRes = R.string.alert_login_failed;
                    break;
            }

            new AlertDialog.Builder(this)
                    .setTitle(getString(titleRes))
                    .setMessage(getString(error))
                    .setPositiveButton(R.string.alert_positive_button, (dialog, which) -> {})
                    .show();
        });
    }
}
