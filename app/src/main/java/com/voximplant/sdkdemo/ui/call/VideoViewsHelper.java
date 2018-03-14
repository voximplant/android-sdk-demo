/*
 * Copyright (c) 2011- 2018, Zingaya, Inc. All rights reserved.
 */

package com.voximplant.sdkdemo.ui.call;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import com.voximplant.sdkdemo.R;

import org.webrtc.SurfaceViewRenderer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.voximplant.sdkdemo.utils.Constants.APP_TAG;

class VideoViewsHelper {
    private static final int LOCAL_X_CONNECTED = 72;
    private static final int LOCAL_Y_CONNECTED = 72;
    private static final int LOCAL_WIDTH_CONNECTED = 25;
    private static final int LOCAL_HEIGHT_CONNECTED = 25;
    private static final int REMOTE_X = 0;
    private static final int REMOTE_Y = 0;
    private static final int REMOTE_WIDTH = 100;
    private static final int REMOTE_HEIGHT = 100;

    private Context mContext;
    private ViewGroup mParentView;
    private SurfaceViewRenderer mLocalVideoView = null;

    // stream id and SurfaceViewRenderers+
    private ConcurrentHashMap<String, SurfaceViewRenderer> mRemoteVideoView = new ConcurrentHashMap<>();

    VideoViewsHelper(Context context, ViewGroup viewGroup) {
        mContext = context;
        mParentView = viewGroup;
    }

    SurfaceViewRenderer addLocalVideoView() {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View localVideoRoot;
        if (layoutInflater != null) {
            localVideoRoot = layoutInflater.inflate(R.layout.video_view, null);
            mParentView.addView(localVideoRoot);
            PercentFrameLayout localVideoViewLayout = localVideoRoot.findViewById(R.id.video_view_layout);
            localVideoViewLayout.setPosition(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED, LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED);
            mLocalVideoView = localVideoRoot.findViewById(R.id.video_view);
            mLocalVideoView.setZOrderMediaOverlay(true);
        }
        return mLocalVideoView;
    }

    SurfaceViewRenderer removeLocalVideoView() {
        if (mLocalVideoView != null) {
            ViewParent viewParent = mLocalVideoView.getParent();
            if (viewParent != null && viewParent instanceof PercentFrameLayout) {
                mParentView.removeView((View) viewParent);
            }
        }
        return mLocalVideoView;
    }

    SurfaceViewRenderer addRemoteVideoView(String id) {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (layoutInflater == null) {
            return null;
        }

        View remoteVideoRoot = layoutInflater.inflate(R.layout.video_view, null);
        mParentView.addView(remoteVideoRoot);
        PercentFrameLayout remoteVideoLayout = remoteVideoRoot.findViewById(R.id.video_view_layout);
        remoteVideoLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);
        SurfaceViewRenderer remoteVideoView = remoteVideoRoot.findViewById(R.id.video_view);

        mRemoteVideoView.put(id, remoteVideoView);

        return remoteVideoView;
    }

    SurfaceViewRenderer removeRemoteVideoView(String id) {
        SurfaceViewRenderer removedRenderer = null;
        for (Map.Entry<String, SurfaceViewRenderer> entry : mRemoteVideoView.entrySet()) {
            if (entry.getKey().equals(id)) {
                ViewParent viewParent = entry.getValue().getParent();
                if (viewParent != null && viewParent instanceof PercentFrameLayout) {
                    removedRenderer = entry.getValue();
                    mParentView.removeView((View)viewParent);
                }
            }
        }
        if (removedRenderer != null) {
            mRemoteVideoView.remove(id);
        }
        return removedRenderer;
    }

    void removeAllVideoViews() {
        removeLocalVideoView();
        for (Map.Entry<String, SurfaceViewRenderer> entry : mRemoteVideoView.entrySet()) {
            removeRemoteVideoView(entry.getKey());
        }
    }

    void showAllViews() {
        mLocalVideoView.setVisibility(View.VISIBLE);
        for (Map.Entry<String, SurfaceViewRenderer> entry : mRemoteVideoView.entrySet()) {
            entry.getValue().setVisibility(View.VISIBLE);
        }
    }

    void hideAllViews() {
        mLocalVideoView.setVisibility(View.INVISIBLE);
        for (Map.Entry<String, SurfaceViewRenderer> entry : mRemoteVideoView.entrySet()) {
            entry.getValue().setVisibility(View.INVISIBLE);
        }
    }
}
