package com.imankur.analogclockview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

import static android.text.format.DateUtils.SECOND_IN_MILLIS;

public class AnalogClock extends View {

    private final BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())) {
                final String tz = intent.getStringExtra("time-zone");
                mTime = Calendar.getInstance(TimeZone.getTimeZone(tz));
            }
            onTimeChanged();
        }
    };

    private final Runnable mClockTick = new Runnable() {
        @Override
        public void run() {
            onTimeChanged();

            if (mEnableSeconds) {
                final long now = System.currentTimeMillis();
                final long delay = SECOND_IN_MILLIS - now % SECOND_IN_MILLIS;
                postDelayed(this, delay);
            }
        }
    };

    private Drawable mDial;
    private Drawable mHourHand;
    private Drawable mMinuteHand;
    private Drawable mSecondHand;

    private Calendar mTime;
    private String mDescFormat;
    private TimeZone mTimeZone;
    private boolean mEnableSeconds = true;

    public AnalogClock(Context context) {
        this(context, null);
    }

    public AnalogClock(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AnalogClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final Resources r = context.getResources();
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AnalogClock);
        mTime = Calendar.getInstance();
        mDescFormat = ((SimpleDateFormat) DateFormat.getTimeFormat(context)).toLocalizedPattern();
        mEnableSeconds = a.getBoolean(R.styleable.AnalogClock_showSecondHand, true);
        mDial = a.getDrawable(R.styleable.AnalogClock_dial);
        if (mDial == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mDial = context.getDrawable(R.drawable.dial);
            } else {
                mDial = r.getDrawable(R.drawable.dial);
            }
        }
        mHourHand = a.getDrawable(R.styleable.AnalogClock_hour);
        if (mHourHand == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mHourHand = context.getDrawable(R.drawable.hour);
            } else {
                mHourHand = r.getDrawable(R.drawable.hour);
            }
        }
        mMinuteHand = a.getDrawable(R.styleable.AnalogClock_minute);
        if (mMinuteHand == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mMinuteHand = context.getDrawable(R.drawable.minute);
            } else {
                mMinuteHand = r.getDrawable(R.drawable.minute);
            }
        }
        mSecondHand = a.getDrawable(R.styleable.AnalogClock_second);
        if (mSecondHand == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mSecondHand = context.getDrawable(R.drawable.second);
            } else {
                mSecondHand = r.getDrawable(R.drawable.second);
            }
        }
        initDrawable(context, mDial);
        initDrawable(context, mHourHand);
        initDrawable(context, mMinuteHand);
        initDrawable(context, mSecondHand);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(mIntentReceiver, filter);
        mTime = Calendar.getInstance(mTimeZone != null ? mTimeZone : TimeZone.getDefault());
        onTimeChanged();
        if (mEnableSeconds) {
            mClockTick.run();
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        getContext().unregisterReceiver(mIntentReceiver);
        removeCallbacks(mClockTick);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int minWidth = Math.max(mDial.getIntrinsicWidth(), getSuggestedMinimumWidth());
        final int minHeight = Math.max(mDial.getIntrinsicHeight(), getSuggestedMinimumHeight());
        setMeasuredDimension(getDefaultSize(minWidth, widthMeasureSpec),
                getDefaultSize(minHeight, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int w = getWidth();
        final int h = getHeight();

        final int saveCount = canvas.save();
        canvas.translate(w / 2, h / 2);
        final float scale = Math.min((float) w / mDial.getIntrinsicWidth(),
                (float) h / mDial.getIntrinsicHeight());
        if (scale < 1f) {
            canvas.scale(scale, scale, 0f, 0f);
        }
        mDial.draw(canvas);

        final float hourAngle = mTime.get(Calendar.HOUR) * 30f;
        canvas.rotate(hourAngle, 0f, 0f);
        mHourHand.draw(canvas);

        final float minuteAngle = mTime.get(Calendar.MINUTE) * 6f;
        canvas.rotate(minuteAngle - hourAngle, 0f, 0f);
        mMinuteHand.draw(canvas);

        if (mEnableSeconds) {
            final float secondAngle = mTime.get(Calendar.SECOND) * 6f;
            canvas.rotate(secondAngle - minuteAngle, 0f, 0f);
            mSecondHand.draw(canvas);
        }
        canvas.restoreToCount(saveCount);
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return mDial == who
                || mHourHand == who
                || mMinuteHand == who
                || mSecondHand == who
                || super.verifyDrawable(who);
    }

    private void initDrawable(Context context, Drawable drawable) {
        final int midX = drawable.getIntrinsicWidth() / 2;
        final int midY = drawable.getIntrinsicHeight() / 2;
        drawable.setBounds(-midX, -midY, midX, midY);
    }

    private void onTimeChanged() {
        mTime.setTimeInMillis(System.currentTimeMillis());
        setContentDescription(DateFormat.format(mDescFormat, mTime));
        invalidate();
    }

    public void setTimeZone(String id) {
        mTimeZone = TimeZone.getTimeZone(id);
        mTime.setTimeZone(mTimeZone);
        onTimeChanged();
    }
}
