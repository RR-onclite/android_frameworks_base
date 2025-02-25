/*
* Copyright (C) 2019 The OmniROM Project
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 2 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package com.android.systemui.rr;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.WallpaperColors;
import android.app.WallpaperInfo;
import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.android.settingslib.Utils;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.tuner.TunerService;

import androidx.palette.graphics.Palette;

public class NotificationLightsView extends RelativeLayout implements TunerService.Tunable {
    private static final boolean DEBUG = false;
    private static final String TAG = "NotificationLightsView";
    private View mNotificationAnimView;
    private ValueAnimator mLightAnimator;
    private boolean mPulsing;
    private WallpaperManager mWallManager;
    public static final String REPEAT_COUNT =
            "system:" + Settings.System.PULSE_AMBIENT_LIGHT_REPEAT_COUNT;
    public static final String LIGHT_DURATION =
            "system:" + Settings.System.PULSE_AMBIENT_LIGHT_DURATION;
    public int mLightsDuration, mCount;

    public NotificationLightsView(Context context) {
        this(context, null);
    }

    public NotificationLightsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NotificationLightsView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public NotificationLightsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        if (DEBUG) Log.d(TAG, "new");
        mLightAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 2.0f});
    }


    private Runnable mLightUpdate = new Runnable() {
        @Override
        public void run() {
            animateNotification();
        }
    };

    public void setPulsing(boolean pulsing) {
        if (mPulsing == pulsing) {
            return;
        }
        mPulsing = pulsing;
    }

    public void stopAnimateNotification() {
        if (mLightAnimator != null) {
            mLightAnimator.end();
            mLightAnimator = null;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
    }

    public void animateNotification() {
        animateNotificationWithColor(getNotificationLightsColor());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(TunerService.class).addTunable(this, REPEAT_COUNT);
        Dependency.get(TunerService.class).addTunable(this, LIGHT_DURATION);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Dependency.get(TunerService.class).removeTunable(this);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        if (REPEAT_COUNT.equals(key)) {
             mCount =
                   TunerService.parseInteger(newValue, 0) ;
        } else if (LIGHT_DURATION.equals(key)) {
            mLightsDuration =
                   TunerService.parseInteger(newValue, 2) * 1000;
        }
    }

    public int getNotificationLightsColor() {
        int color = getDefaultNotificationLightsColor();
        int lightColor = getlightColor();
        int customColor = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_LIGHT_CUSTOM_COLOR, getDefaultNotificationLightsColor(),
                UserHandle.USER_CURRENT);
        int blend = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_LIGHT_BLEND_COLOR, getDefaultNotificationLightsColor(),
                UserHandle.USER_CURRENT);
        if (lightColor == 1) {
            try {
                WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
                WallpaperInfo wallpaperInfo = wallpaperManager.getWallpaperInfo();
                if (wallpaperInfo == null) { // if not a live wallpaper
                    Drawable wallpaperDrawable = wallpaperManager.getDrawable();
                    Bitmap bitmap = ((BitmapDrawable)wallpaperDrawable).getBitmap();
                    if (bitmap != null) { // if wallpaper is not blank
                        Palette p = Palette.from(bitmap).generate();
                        int wallColor = p.getDominantColor(color);
                        if (color != wallColor)
                            color = wallColor;
                    }
                }
            } catch (Exception e) {
                // Nothing to do
            }
        } else if (lightColor == 2) {
            color = Utils.getColorAccentDefaultColor(getContext());
        } else if (lightColor == 3) {
            color = customColor;
        } else if (lightColor == 4) {
            color = mixColors(customColor, blend);
        } else if (lightColor == 5) {
            color = randomColor();
        }   else {
            color = 0xFFFFFFFF;
        }
        return color;
    }

    public int getDefaultNotificationLightsColor() {
        return getResources().getInteger(com.android.internal.R.integer.config_AmbientPulseLightColor);
    }

    public void endAnimation() {
        mLightAnimator.end();
        mLightAnimator.removeAllUpdateListeners();
    }

    public int randomColor() {
        int red = (int) (0xff * Math.random());
        int green = (int) (0xff * Math.random());
        int blue = (int) (0xff * Math.random());
        return Color.argb(255, red, green, blue);
    }

    private int mixColors(int color1, int color2) {
        int[] rgb1 = colorToRgb(color1);
        int[] rgb2 = colorToRgb(color2);

        rgb1[0] = mixedValue(rgb1[0], rgb2[0]);
        rgb1[1] = mixedValue(rgb1[1], rgb2[1]);
        rgb1[2] = mixedValue(rgb1[2], rgb2[2]);
        rgb1[3] = mixedValue(rgb1[3], rgb2[3]);

        return rgbToColor(rgb1);
    }

    private int[] colorToRgb(int color) {
        int[] rgb = {(color & 0xFF000000) >> 24, (color & 0xFF0000) >> 16, (color & 0xFF00) >> 8, (color & 0xFF)};
        return rgb;
    }

    private int rgbToColor(int[] rgb) {
        return (rgb[0] << 24) + (rgb[1] << 16) + (rgb[2] << 8) + rgb[3];
    }

    private int mixedValue(int val1, int val2) {
        return (int)Math.min((val1 + val2), 255f);
    }

    public int getlightColor() {
        return Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.AMBIENT_LIGHT_COLOR, 0,
                UserHandle.USER_CURRENT);
    }

    public void animateNotificationWithColor(int color) {
        boolean directionIsRestart = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.PULSE_AMBIENT_LIGHT_REPEAT_DIRECTION, 0,
                UserHandle.USER_CURRENT) != 1;
        int width = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.PULSE_AMBIENT_LIGHT_WIDTH, 125,
                UserHandle.USER_CURRENT);
        int layout = Settings.System.getIntForUser(mContext.getContentResolver(),
                Settings.System.PULSE_AMBIENT_LIGHT_LAYOUT, 0,
                UserHandle.USER_CURRENT);
        int lightcolor = getlightColor();
        ImageView leftViewSolid = (ImageView) findViewById(R.id.notification_animation_left_solid);
        ImageView leftViewFaded = (ImageView) findViewById(R.id.notification_animation_left_faded);
        if (lightcolor == 6) {
            leftViewSolid.setColorFilter(randomColor());
            leftViewFaded.setColorFilter(randomColor());
        } else {
            leftViewSolid.setColorFilter(color);
            leftViewFaded.setColorFilter(color);
        }
        leftViewSolid.getLayoutParams().width = width;
        leftViewFaded.getLayoutParams().width = width;
        leftViewSolid.setVisibility(layout == 0 ? View.VISIBLE : View.GONE);
        leftViewFaded.setVisibility(layout == 1 ? View.VISIBLE : View.GONE);
        ImageView rightViewSolid = (ImageView) findViewById(R.id.notification_animation_right_solid);
        ImageView rightViewFaded = (ImageView) findViewById(R.id.notification_animation_right_faded);
        if (lightcolor == 6) {
            rightViewSolid.setColorFilter(randomColor());
            rightViewFaded.setColorFilter(randomColor());
        } else {
           rightViewSolid.setColorFilter(color);
           rightViewFaded.setColorFilter(color);
        }
        rightViewSolid.getLayoutParams().width = width;
        rightViewFaded.getLayoutParams().width = width;
        rightViewSolid.setVisibility(layout == 0 ? View.VISIBLE : View.GONE);
        rightViewFaded.setVisibility(layout == 1 ? View.VISIBLE : View.GONE);
        mLightAnimator = ValueAnimator.ofFloat(new float[]{0.0f, 2.0f});
        mLightAnimator.setDuration(mLightsDuration);
        if (mCount == 0) {
            mLightAnimator.setRepeatCount(ValueAnimator.INFINITE);
        } else {
            mLightAnimator.setRepeatCount(mCount - 1);
        }
        mLightAnimator.setRepeatMode(directionIsRestart ? ValueAnimator.RESTART : ValueAnimator.REVERSE);
        mLightAnimator.addUpdateListener(new AnimatorUpdateListener() {
            public void onAnimationUpdate(ValueAnimator animation) {
                float progress = ((Float) animation.getAnimatedValue()).floatValue();
                leftViewSolid.setScaleY(progress);
                leftViewFaded.setScaleY(progress);
                rightViewSolid.setScaleY(progress);
                rightViewFaded.setScaleY(progress);
                float alpha = 1.0f;
                if (progress <= 0.3f) {
                    alpha = progress / 0.3f;
                } else if (progress >= 1.0f) {
                    alpha = 2.0f - progress;
                }
                leftViewSolid.setAlpha(alpha);
                leftViewFaded.setAlpha(alpha);
                rightViewSolid.setAlpha(alpha);
                rightViewFaded.setAlpha(alpha);
            }
        });
       if (DEBUG) Log.d(TAG, "start");
        mLightAnimator.start();
    }
}
