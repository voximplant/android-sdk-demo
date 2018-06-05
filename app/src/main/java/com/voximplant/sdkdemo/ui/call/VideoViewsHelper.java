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
    private static final int REMOTE_PADDING = 10;

    private Context mContext;
    private ViewGroup mParentView;
    private SurfaceViewRenderer mLocalVideoView = null;

    // stream id and SurfaceViewRenderers
    private ConcurrentHashMap<String, SurfaceViewRenderer> mRemoteVideoView = new ConcurrentHashMap<>();
    // stream id and display name
    private ConcurrentHashMap<String, String> mRemoteVideoInfo = new ConcurrentHashMap<>();

    private int mRemoteViewColumns;
    private int mRemoteViewRows;

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
            localVideoViewLayout.setPosition(LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
                    LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED, 0);
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

    SurfaceViewRenderer addRemoteVideoView(String id, String displayName) {
        LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (layoutInflater == null) {
            return null;
        }
        calculateRemoteVideoColumnsAndRows(mRemoteVideoView.size() + 1);
        View remoteVideoRoot = layoutInflater.inflate(R.layout.video_view, null);
        mParentView.addView(remoteVideoRoot);
        PercentFrameLayout remoteVideoLayout = remoteVideoRoot.findViewById(R.id.video_view_layout);
        remoteVideoLayout.setPosition(REMOTE_X, REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT, REMOTE_PADDING);
        SurfaceViewRenderer remoteVideoView = remoteVideoRoot.findViewById(R.id.video_view);
        TextView nameView = remoteVideoRoot.findViewById(R.id.endpoint_name);
        nameView.setText(displayName);

        mRemoteVideoView.put(id, remoteVideoView);
        mRemoteVideoInfo.put(id, displayName == null ? "" : displayName);

        rearrangeRemoteViews();

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
            mRemoteVideoInfo.remove(id);
        }
        calculateRemoteVideoColumnsAndRows(mRemoteVideoView.size());
        rearrangeRemoteViews();
        return removedRenderer;
    }

    void removeAllVideoViews() {
        removeLocalVideoView();
        for (Map.Entry<String, SurfaceViewRenderer> entry : mRemoteVideoView.entrySet()) {
            removeRemoteVideoView(entry.getKey());
        }
    }

    void showAllViews() {
        if (mLocalVideoView != null) {
            mLocalVideoView.setVisibility(View.VISIBLE);
        }
        for (Map.Entry<String, SurfaceViewRenderer> entry : mRemoteVideoView.entrySet()) {
            entry.getValue().setVisibility(View.VISIBLE);
        }
    }

    void hideAllViews() {
        if (mLocalVideoView != null) {
            mLocalVideoView.setVisibility(View.INVISIBLE);
        }
        for (Map.Entry<String, SurfaceViewRenderer> entry : mRemoteVideoView.entrySet()) {
            entry.getValue().setVisibility(View.INVISIBLE);
        }
    }

    void updateRemoteVideoView(String id, String displayName) {
        SurfaceViewRenderer videoRenderer = mRemoteVideoView.get(id);
        if (videoRenderer != null) {
            ViewParent parent = videoRenderer.getParent();
            TextView textView = ((View)parent).findViewById(R.id.endpoint_name);
            textView.setText(displayName);
            mRemoteVideoInfo.put(id, displayName);
        }
    }

    private void calculateRemoteVideoColumnsAndRows(int count) {
        int columns = 1;
        int rows = 1;
        if (count > 0 && count < 17) {
            columns = 4;
            int m = 3;
            while ((count -1) / columns < m) {
                columns--;
                m--;
            }
            rows = ((count - 1) / columns) + 1;
        }

        mRemoteViewColumns = columns;
        mRemoteViewRows = rows;

        Log.i(APP_TAG, "calculateRemoteVideoColumnsAndRows: columns = " + mRemoteViewColumns + ", rows = " + mRemoteViewRows);
    }

    private void rearrangeRemoteViews() {
        int remote_x;
        int remote_y;
        int remote_y_coef = 0;

        int i = 0;
        for (Map.Entry<String, SurfaceViewRenderer> entry : mRemoteVideoView.entrySet()) {
            remote_x = REMOTE_X + ((i % mRemoteViewColumns) * 100 / mRemoteViewColumns);
            remote_y = REMOTE_Y + (remote_y_coef * 100 / mRemoteViewRows);
            if (remote_x + (100 / mRemoteViewColumns) >= 99) {
                remote_y_coef++;
            }
            PercentFrameLayout parent = (PercentFrameLayout) entry.getValue().getParent();
            parent.setPosition(remote_x, remote_y,
                    REMOTE_WIDTH / mRemoteViewColumns, REMOTE_HEIGHT / mRemoteViewRows, REMOTE_PADDING);
            parent.setVisibility(View.VISIBLE);

            Log.i(APP_TAG, "rearrangeRemoteViews: " + i + ", remote_x = " + remote_x + ", remote_y = " + remote_y  +
                    ", remote_width = " + REMOTE_WIDTH / mRemoteViewColumns + ", remote_height = " + REMOTE_HEIGHT / mRemoteViewRows);

            i++;
        }
    }
}
