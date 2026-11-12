package com.liskovsoft.smartyoutubetv2.tv.util;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.style.ReplacementSpan;

/**
 * YouTube color scheme: a quality badge (4K, CC) drawn as a small rounded chip inside a text line
 */
public class ChipSpan extends ReplacementSpan {
    private final int mBackgroundColor;
    private final int mTextColor;
    private final float mPadding;
    private final float mRadius;
    private final RectF mRect = new RectF();

    public ChipSpan(int backgroundColor, int textColor, float padding, float radius) {
        mBackgroundColor = backgroundColor;
        mTextColor = textColor;
        mPadding = padding;
        mRadius = radius;
    }

    @Override
    public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
        return Math.round(paint.measureText(text, start, end) + mPadding * 2);
    }

    @Override
    public void draw(Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
        Paint.FontMetrics metrics = paint.getFontMetrics();
        mRect.set(x, y + metrics.ascent, x + paint.measureText(text, start, end) + mPadding * 2, y + metrics.descent);
        int color = paint.getColor();
        paint.setColor(mBackgroundColor);
        canvas.drawRoundRect(mRect, mRadius, mRadius, paint);
        paint.setColor(mTextColor);
        canvas.drawText(text, start, end, x + mPadding, y, paint);
        paint.setColor(color);
    }
}
