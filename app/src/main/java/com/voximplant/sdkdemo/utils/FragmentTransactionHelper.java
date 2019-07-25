/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.utils;

import androidx.fragment.app.Fragment;
import android.content.Context;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;

public class FragmentTransactionHelper {
    private FragmentActivity mContext;
    private ArrayList<Fragment> mFragments = new ArrayList<>();

    public FragmentTransactionHelper(Context context) {
        mContext = (FragmentActivity) context;
    }

    public void addFragment(Fragment fragment, String tag, int resContainer) {
        if (fragment != null) {
            mContext.getSupportFragmentManager()
                    .beginTransaction()
                    .add(resContainer, fragment, tag)
                    .commit();
            mFragments.add(fragment);
        }
    }

    public void showFragment(String tag) {
        showFragment(mContext.getSupportFragmentManager().findFragmentByTag(tag));
    }

    private void showFragment(Fragment fragment) {
        if (fragment != null) {
            mContext.getSupportFragmentManager().beginTransaction().show(fragment).commit();
        }
    }

    private void hideFragment(Fragment fragment) {
        if (fragment != null) {
            mContext.getSupportFragmentManager().beginTransaction().hide(fragment).commit();
        }
    }

    public void removeFragment(String tag) {
        Fragment fragment = mContext.getSupportFragmentManager().findFragmentByTag(tag);
        removeFragment(fragment);
    }

    private void removeFragment(Fragment fragment) {
        if (fragment != null) {
            mContext.getSupportFragmentManager().beginTransaction().remove(fragment).commit();
            mFragments.remove(fragment);
        }
    }

    public void findAndHideVisibleFragment() {
        for (Fragment fragment : mFragments) {
            if (fragment != null && fragment.isVisible()) {
                hideFragment(fragment);
            }
        }
    }

    public void showActiveCallFragment() {
        for (Fragment fragment : mFragments) {
            if (fragment != null && fragment.isHidden()) {
                showFragment(fragment);
                break;
            }
        }
    }
}
