/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import java.io.File;
import java.io.IOException;

import android.content.Context;
import android.content.ContentResolver;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.MotionEvent;
import android.view.View;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;

import com.android.systemui.R;
import com.android.systemui.statusbar.GestureRecorder;

public class NotificationPanelView extends PanelView {

   
    private final String NOTIF_WALLPAPER_IMAGE_PATH = "/data/data/com.teamsourcery.sourcerytools/files/notification_wallpaper.jpg";
    private final String NOTIF_WALLPAPER_IMAGE_PATH_LAND = "/data/data/com.teamsourcery.sourcerytools/files/notification_wallpaper_land.jpg";
    private final boolean hasPortrait = new File(NOTIF_WALLPAPER_IMAGE_PATH).exists();
    private final boolean hasLandscape = new File(NOTIF_WALLPAPER_IMAGE_PATH_LAND).exists();
    private static final float STATUS_BAR_SETTINGS_LEFT_PERCENTAGE = 0.8f;
    private static final float STATUS_BAR_SETTINGS_RIGHT_PERCENTAGE = 0.2f;
    private static final float STATUS_BAR_SWIPE_TRIGGER_PERCENTAGE = 0.05f;
    private static final float STATUS_BAR_SWIPE_VERTICAL_MAX_PERCENTAGE = 0.025f;
    private static final float STATUS_BAR_SWIPE_MOVE_PERCENTAGE = 0.2f;

    private static final float STATUS_BAR_SETTINGS_FLIP_PERCENTAGE_RIGHT = 0.15f;
    private static final float STATUS_BAR_SETTINGS_FLIP_PERCENTAGE_LEFT = 0.85f;

    private int mScreenOrientation;

    float wallpaperAlpha = Settings.System.getFloat(getContext()
             .getContentResolver(), Settings.System.NOTIF_WALLPAPER_ALPHA, 1.0f);

    Drawable mHandleBar;
    float mHandleBarHeight;
    View mHandleView;
    int mFingers;
    PhoneStatusBar mStatusBar;
    boolean mOkToFlip;

    float mGestureStartX;
    float mGestureStartY;
    float mFlipOffset;
    float mSwipeDirection;
    boolean mTrackingSwipe;
    boolean mSwipeTriggered;

    
    boolean mFastToggleEnabled;
    int mFastTogglePos;
    ContentObserver mEnableObserver;
    ContentObserver mChangeSideObserver;
    int mToggleStyle;
    Handler mHandler = new Handler();

    public NotificationPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);

     
     }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    	setNotificationWallpaper();
    }

    public void setNotificationWallpaper() {
    mScreenOrientation = getContext().getResources().getConfiguration().orientation;
        boolean isPortrait =  mScreenOrientation == Configuration.ORIENTATION_PORTRAIT;

        if (isPortrait) {
            if (hasPortrait) {
                Drawable d = Drawable.createFromPath(NOTIF_WALLPAPER_IMAGE_PATH);
                d.setAlpha((int) (wallpaperAlpha * 255));
                this.setBackground(d);
	    } else {
                this.setBackground(this.getResources().getDrawable(R.drawable.sourcery_animation));
            }
       } else {
            if (hasLandscape) {
                Drawable d = Drawable.createFromPath(NOTIF_WALLPAPER_IMAGE_PATH_LAND);
                d.setAlpha((int) (wallpaperAlpha * 255));
                this.setBackground(d);
       } else {
                this.setBackground(this.getResources().getDrawable(R.drawable.sourcery_animation));
            }
       }
   }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
    //super.onConfigurationChanged(newConfig);
    if (newConfig.orientation != mScreenOrientation) {
            if (hasPortrait || hasLandscape) {
                setNotificationWallpaper();
            }
        }
    }

    public void setStatusBar(PhoneStatusBar bar) {
        mStatusBar = bar;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Resources resources = getContext().getResources();
        mHandleBar = resources.getDrawable(R.drawable.status_bar_close);
        mHandleBarHeight = resources.getDimensionPixelSize(R.dimen.close_handle_height);
        mHandleView = findViewById(R.id.handle);

        setContentDescription(resources.getString(R.string.accessibility_desc_notification_shade));
    

     final ContentResolver resolver = getContext().getContentResolver();
        mEnableObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                mFastToggleEnabled = Settings.System.getBoolean(resolver,
                        Settings.System.FAST_TOGGLE, false);
                mToggleStyle = Settings.System.getInt(resolver,
                        Settings.System.TOGGLES_STYLE, 0);
            }
        };

        mChangeSideObserver = new ContentObserver(mHandler) {
            @Override
            public void onChange(boolean selfChange) {
                mFastTogglePos = Settings.System.getInt(resolver,
                        Settings.System.CHOOSE_FASTTOGGLE_SIDE, 1);
            }
        };

        // Initialization
        mFastToggleEnabled = Settings.System.getBoolean(resolver,
                Settings.System.FAST_TOGGLE, false);
        mFastTogglePos = Settings.System.getInt(resolver,
                Settings.System.CHOOSE_FASTTOGGLE_SIDE, 1);
        mToggleStyle = Settings.System.getInt(resolver,
                Settings.System.TOGGLES_STYLE, 0);

        resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.FAST_TOGGLE),
                true, mEnableObserver);
        resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.TOGGLES_STYLE),
                true, mEnableObserver);

        resolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.CHOOSE_FASTTOGGLE_SIDE),
                true, mChangeSideObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        getContext().getContentResolver().unregisterContentObserver(mEnableObserver);
        getContext().getContentResolver().unregisterContentObserver(mChangeSideObserver);
        super.onDetachedFromWindow();
    }


    @Override
    public void fling(float vel, boolean always) {
        GestureRecorder gr = ((PhoneStatusBarView) mBar).mBar.getGestureRecorder();
        if (gr != null) {
            gr.tag(
                "fling " + ((vel > 0) ? "open" : "closed"),
                "notifications,v=" + vel);
        }
        super.fling(vel, always);
    }

    // We draw the handle ourselves so that it's always glued to the bottom of the window.
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            final int pl = getPaddingLeft();
            final int pr = getPaddingRight();
            mHandleBar.setBounds(pl, 0, getWidth() - pr, (int) mHandleBarHeight);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        final int off = (int) (getHeight() - mHandleBarHeight - getPaddingBottom());
        canvas.translate(0, off);
        mHandleBar.setState(mHandleView.getDrawableState());
        mHandleBar.draw(canvas);
        canvas.translate(0, -off);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean shouldRecycleEvent = false;
        if (PhoneStatusBar.SETTINGS_DRAG_SHORTCUT && mStatusBar.mHasFlipSettings) {
            boolean flip = false;
            boolean shouldFlip = false;
            boolean swipeFlipJustFinished = false;
            boolean swipeFlipJustStarted = false;
            boolean noNotificationPulldown = Settings.System.getInt(getContext().getContentResolver(),
                                    Settings.System.QS_NO_NOTIFICATION_PULLDOWN, 0) == 1;
            int quickPulldownMode = Settings.System.getInt(getContext().getContentResolver(),
 	                            Settings.System.QS_QUICK_PULLDOWN, 0);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    mGestureStartX = event.getX(0);
                    mGestureStartY = event.getY(0);
                    mTrackingSwipe = isFullyExpanded() &&
                        // Pointer is at the handle portion of the view?
                        mGestureStartY > getHeight() - mHandleBarHeight - getPaddingBottom();
                    mOkToFlip = getExpandedHeight() == 0;
                    if(mToggleStyle != 0) {
                        // don't allow settings panel with non-tile toggles
                        shouldFlip = false;
                        break;
                    }
                    if (event.getX(0) > getWidth() * (1.0f - STATUS_BAR_SETTINGS_RIGHT_PERCENTAGE) &&
                            quickPulldownMode == 1) {
                        flip = true;
                    } else if (event.getX(0) < getWidth() * (1.0f - STATUS_BAR_SETTINGS_LEFT_PERCENTAGE) &&
                             quickPulldownMode == 2) {
                        flip = true;
 	            } else if (!mStatusBar.hasClearableNotifications() && noNotificationPulldown) {
                        flip = true;
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    final float deltaX = Math.abs(event.getX(0) - mGestureStartX);
                    final float deltaY = Math.abs(event.getY(0) - mGestureStartY);
                    final float maxDeltaY = getHeight() * STATUS_BAR_SWIPE_VERTICAL_MAX_PERCENTAGE;
                    final float minDeltaX = getWidth() * STATUS_BAR_SWIPE_TRIGGER_PERCENTAGE;
                    if (mTrackingSwipe && deltaY > maxDeltaY) {
                        mTrackingSwipe = false;
                    }
                    if (mTrackingSwipe && deltaX > deltaY && deltaX > minDeltaX) {

                        // The value below can be used to adjust deltaX to always increase,
                        // if the user keeps swiping in the same direction as she started the
                        // gesture. If she, however, moves her finger the other way, deltaX will
                        // decrease.
                        //
                        // This allows for an horizontal swipe, in any direction, to always flip
                        // the views.
                        mSwipeDirection = event.getX(0) < mGestureStartX ? -1f : 1f;

                        if (mStatusBar.isShowingSettings()) {
                            mFlipOffset = 1f;
                            // in this case, however, we need deltaX to decrease
                            mSwipeDirection = -mSwipeDirection;
                        } else {
                            mFlipOffset = -1f;
                        }
                        mGestureStartX = event.getX(0);
                        mTrackingSwipe = false;
                        mSwipeTriggered = true;
                        swipeFlipJustStarted = true;
                    }
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    flip = true;
                    break;
                case MotionEvent.ACTION_UP:
                    swipeFlipJustFinished = mSwipeTriggered;
                    mSwipeTriggered = false;
                    mTrackingSwipe = false;
                    break;
            }
            if (mOkToFlip && flip) {
                float miny = event.getY(0);
                float maxy = miny;
                for (int i=1; i<event.getPointerCount(); i++) {
                    final float y = event.getY(i);
                    if (y < miny) miny = y;
                    if (y > maxy) maxy = y;
                }
                if (maxy - miny < mHandleBarHeight) {
                    if (getMeasuredHeight() < mHandleBarHeight) {
                        mStatusBar.switchToSettings();
                    } else {
                        // Do not flip if the drag event started within the top bar
                    if (MotionEvent.ACTION_DOWN == event.getActionMasked() && event.getY(0) < mHandleBarHeight ) {
                        mStatusBar.switchToSettings();
                    } else {
                        mStatusBar.flipToSettings();
                    }
                    }
                    mOkToFlip = false;
                }
            } else if (mSwipeTriggered) {
                final float deltaX = (event.getX(0) - mGestureStartX) * mSwipeDirection;
                mStatusBar.partialFlip(mFlipOffset +
                                       deltaX / (getWidth() * STATUS_BAR_SWIPE_MOVE_PERCENTAGE));
                if (!swipeFlipJustStarted) {
                    return true; // Consume the event.
                }
            } else if (swipeFlipJustFinished) {
                mStatusBar.completePartialFlip();
            }

            if (swipeFlipJustStarted || swipeFlipJustFinished) {
                // Made up event: finger at the middle bottom of the view.
                MotionEvent original = event;
                event = MotionEvent.obtain(original.getDownTime(), original.getEventTime(),
                    original.getAction(), getWidth()/2, getHeight(),
                    original.getPressure(0), original.getSize(0), original.getMetaState(),
                    original.getXPrecision(), original.getYPrecision(), original.getDeviceId(),
                    original.getEdgeFlags());

                // The following two lines looks better than the chunk of code above, but,
                // nevertheless, doesn't work. The view is not pinned down, and may close,
                // just after the gesture is finished.
                //
                // event = MotionEvent.obtainNoHistory(original);
                // event.setLocation(getWidth()/2, getHeight());
                shouldRecycleEvent = true;
            }

        }
        final boolean result = mHandleView.dispatchTouchEvent(event);
        if (shouldRecycleEvent) {
            event.recycle();
        }
        return result;
    }
}
