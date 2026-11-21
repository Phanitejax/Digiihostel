package com.digiihostel.app;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

/**
 * Dynamic blueprint background: cycling gradient and drifting grid.
 * The first screen after the app starts gets a light sweep.
 */
public class AnimatedBackgroundView extends View {
    static boolean firstLaunch = true;

    final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    final int[][] pal = {{0xFF061A4A, 0xFF0A4A9E}, {0xFF0B2A6F, 0xFF0E7FBF}, {0xFF1A0F5E, 0xFF0A5FA8}};
    final ArgbEvaluator ev = new ArgbEvaluator();
    float t, sweep;
    ValueAnimator va, in;
    final boolean grand;

    public AnimatedBackgroundView(Context c) {
        super(c);
        grand = firstLaunch;
        firstLaunch = false;
        va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(15000);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(a -> { t = (Float) a.getAnimatedValue(); invalidate(); });
        in = ValueAnimator.ofFloat(0f, 1f);
        in.setDuration(grand ? 2600 : 900);
        in.setInterpolator(new DecelerateInterpolator(1.4f));
        in.addUpdateListener(a -> sweep = (Float) a.getAnimatedValue());
    }

    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); va.start(); in.start(); }
    @Override protected void onDetachedFromWindow() { va.cancel(); in.cancel(); super.onDetachedFromWindow(); }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
        float ph = t * pal.length;
        int i = ((int) ph) % pal.length, j = (i + 1) % pal.length;
        float f = ph - (int) ph;
        int c1 = (Integer) ev.evaluate(f, pal[i][0], pal[j][0]);
        int c2 = (Integer) ev.evaluate(f, pal[i][1], pal[j][1]);
        double a = t * 2 * Math.PI;
        float cx = w / 2, cy = h / 2, dx = (float) Math.cos(a) * w / 2, dy = (float) Math.sin(a) * h / 2;
        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy, c1, c2, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);
        // blueprint grid drifting diagonally
        float step = 48 * d, off = (t * step * 2) % step;
        p.setColor(0x22FFFFFF);
        p.setStrokeWidth(1f);
        for (float x = -step + off; x < w; x += step) c.drawLine(x, 0, x, h, p);
        for (float y = -step + off; y < h; y += step) c.drawLine(0, y, w, y, p);

        // one-off light sweep across the screen on the very first launch
        if (grand && sweep < 1f) {
            float sx = -w * 0.3f + sweep * w * 1.6f;
            p.setStyle(Paint.Style.FILL);
            p.setShader(new LinearGradient(sx - 90 * d, 0, sx + 90 * d, 0,
                    new int[]{0x00FFFFFF, 0x44B3E5FC, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
            c.drawRect(sx - 90 * d, 0, sx + 90 * d, h, p);
            p.setShader(null);
        }
    }
}
