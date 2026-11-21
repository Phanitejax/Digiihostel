package com.digiihostel.app;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import java.util.Calendar;
import java.util.Locale;

/** Every screen = animated background + scrolling column (root). Subclasses fill it in build(). */
abstract class BaseActivity extends Activity {
    LinearLayout root;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        Store.seed(this);
        FrameLayout f = new FrameLayout(this);
        f.addView(new AnimatedBackgroundView(this));
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(40), dp(16), dp(24));
        sv.addView(root);
        f.addView(sv);
        setContentView(f);
        build();
        cascadeIn(root);
    }

    /** Opening animation: every top-level block slides up and fades in one after another. */
    void cascadeIn(ViewGroup g) {
        int n = Math.min(g.getChildCount(), 14);
        for (int i = 0; i < n; i++) {
            View v = g.getChildAt(i);
            if ("noCascade".equals(v.getTag())) continue;     // views that run their own opening animation
            v.setAlpha(0f);
            v.setTranslationY(dp(50));
            v.setScaleX(0.94f); v.setScaleY(0.94f);
            v.animate().alpha(1f).translationY(0).scaleX(1f).scaleY(1f).setStartDelay(120 + i * 70L).setDuration(520)
                    .setInterpolator(new OvershootInterpolator(0.9f)).start();
        }
    }

    /** Left sidebar + right content area (used by Account Manager and Student Details). */
    LinearLayout side, main;
    void splitLayout(int sideDp) {
        FrameLayout f = new FrameLayout(this);
        f.addView(new AnimatedBackgroundView(this));
        LinearLayout h = new LinearLayout(this);
        h.setPadding(dp(8), dp(40), dp(8), dp(8));
        side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        ScrollView ss = new ScrollView(this), ms = new ScrollView(this);
        ss.addView(side);
        ms.addView(main);
        h.addView(ss, new LinearLayout.LayoutParams(dp(sideDp), -1));
        h.addView(ms, new LinearLayout.LayoutParams(0, -1, 1));
        f.addView(h);
        setContentView(f);
    }

    TextView sideItem(String label, boolean selected, View.OnClickListener l) {
        TextView t = text(label, 14, true);
        t.setPadding(dp(8), dp(12), dp(8), dp(12));
        t.setBackground(box(selected ? 0xAA4FC3F7 : 0x33FFFFFF, 0x6680D8FF));
        t.setOnClickListener(l);
        return t;
    }

    // ---------- input rules ----------
    /** Only digits, at most max of them, numeric keyboard. */
    void digitsOnly(EditText e, int max) {
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max), (src, st, en, d, ds, de) -> {
            StringBuilder b = new StringBuilder();
            for (int i = st; i < en; i++) if (Character.isDigit(src.charAt(i))) b.append(src.charAt(i));
            return b.length() == en - st ? null : b.toString();
        }});
    }

    /** Shows a live error while the field contains a space (spaces are never valid in usernames/passwords). */
    void noSpaces(final EditText e) {
        e.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable s) {
                if (Store.hasSpace(s.toString())) e.setError("Spaces are not allowed"); else e.setError(null);
            }
        });
    }

    boolean looksLikeEmail(String s) {
        return !s.isEmpty() && !Store.hasSpace(s) && android.util.Patterns.EMAIL_ADDRESS.matcher(s).matches();
    }

    /** Scrolling (spinner-wheel) date picker. Calls back with yyyy-MM-dd. */
    interface DateCb { void got(String date); }
    void pickDate(String current, boolean allowFuture, int fallbackYear, final DateCb cb) {
        Calendar c = Calendar.getInstance();
        c.set(fallbackYear, Calendar.JANUARY, 1);
        try {
            String[] p = current.split("-");
            c.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]));
        } catch (Exception ignored) { }
        final DatePicker dp = new DatePicker(new ContextThemeWrapper(this, android.R.style.Theme_Holo_Light_Dialog));
        dp.setCalendarViewShown(false);
        dp.setSpinnersShown(true);
        Calendar min = Calendar.getInstance();
        min.set(1950, Calendar.JANUARY, 1);
        dp.setMinDate(min.getTimeInMillis());
        if (!allowFuture) dp.setMaxDate(System.currentTimeMillis());
        dp.updateDate(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        FrameLayout w = new FrameLayout(this);
        w.addView(dp, new FrameLayout.LayoutParams(-2, -2, android.view.Gravity.CENTER));
        new AlertDialog.Builder(this).setTitle("Slide to choose the date").setView(w)
                .setPositiveButton("OK", (d, x) -> cb.got(String.format(Locale.US, "%04d-%02d-%02d", dp.getYear(), dp.getMonth() + 1, dp.getDayOfMonth())))
                .setNegativeButton("Cancel", null).show();
    }

    /** A read-only field that opens the date picker when tapped. */
    EditText dateField(String hint, boolean allowFuture, int fallbackYear) {
        final EditText e = edit(hint);
        e.setFocusable(false);
        e.setCursorVisible(false);
        e.setOnClickListener(v -> pickDate(e.getText().toString(), allowFuture, fallbackYear, d -> { e.setText(d); e.setError(null); }));
        return e;
    }

    /** Turns a password field into a hidden one with an eye icon at the end: tap to show / hide. */
    void eyeToggle(final EditText e, final int tint) {
        final boolean[] shown = {false};
        final Runnable apply = () -> {
            int sel = e.getSelectionEnd();
            e.setInputType(InputType.TYPE_CLASS_TEXT | (shown[0] ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD : InputType.TYPE_TEXT_VARIATION_PASSWORD));
            e.setSingleLine();
            android.graphics.drawable.Drawable d = getResources().getDrawable(shown[0] ? R.drawable.ic_eye : R.drawable.ic_eye_off, getTheme()).mutate();
            d.setTint(tint);
            e.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, d, null);
            e.setSelection(Math.max(0, Math.min(sel, e.length())));
        };
        apply.run();
        e.setOnTouchListener((v, ev) -> {
            if (ev.getAction() == android.view.MotionEvent.ACTION_UP && e.getCompoundDrawablesRelative()[2] != null) {
                boolean rtl = e.getLayoutDirection() == View.LAYOUT_DIRECTION_RTL;
                float edge = e.getCompoundDrawablesRelative()[2].getBounds().width() + e.getPaddingEnd() + dp(8);
                boolean hit = rtl ? ev.getX() <= edge : ev.getX() >= e.getWidth() - edge;
                if (hit) { shown[0] = !shown[0]; apply.run(); v.performClick(); return true; }
            }
            return false;
        });
    }

    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    abstract void build();

    int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }

    TextView text(String s, int sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(sp); t.setTextColor(Color.WHITE);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    GradientDrawable box(int fill, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill); g.setCornerRadius(dp(14)); g.setStroke(dp(1), stroke);
        return g;
    }

    Button button(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s); b.setTextColor(Color.WHITE); b.setAllCaps(false);
        b.setBackground(box(0x884FC3F7, 0xFF4FC3F7));
        b.setOnClickListener(l);
        return b;
    }

    EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setTextColor(Color.WHITE); e.setHintTextColor(0x99FFFFFF);
        e.setBackground(box(0x33FFFFFF, 0x66FFFFFF));
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        return e;
    }

    /** Dropdown adapter that wraps long names on several lines; item 0 can be a grey hint ("Select school"). */
    static class HintAdapter extends ArrayAdapter<String> {
        final boolean hint;
        final float d;
        HintAdapter(android.content.Context c, java.util.List<String> items, boolean hint) {
            super(c, android.R.layout.simple_spinner_dropdown_item, items);
            this.hint = hint;
            d = c.getResources().getDisplayMetrics().density;
        }
        View style(View v, int pos) {
            TextView t = (TextView) v;
            t.setSingleLine(false); t.setMaxLines(4); t.setEllipsize(null);
            t.setTextColor(hint && pos == 0 ? 0xFF757575 : 0xFF111111);
            t.setPadding((int) (12 * d), (int) (10 * d), (int) (12 * d), (int) (10 * d));
            return v;
        }
        @Override public View getView(int p, View c, ViewGroup g) { return style(super.getView(p, c, g), p); }
        @Override public View getDropDownView(int p, View c, ViewGroup g) { return style(super.getDropDownView(p, c, g), p); }
    }

    Spinner spinner(String... items) { return spinnerOf(java.util.Arrays.asList(items), false); }

    Spinner spinnerOf(java.util.List<String> items, boolean firstIsHint) {
        Spinner s = new Spinner(this);
        s.setAdapter(new HintAdapter(this, items, firstIsHint));
        s.setBackground(box(0xDDFFFFFF, 0xFFFFFFFF));
        return s;
    }

    LinearLayout.LayoutParams lp(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.topMargin = dp(top);
        return p;
    }
    void add(View v) { root.addView(v, lp(8)); }
    static String cap(String k) { return k.substring(0, 1).toUpperCase() + k.substring(1); }
    static int statusColor(String s) {
        switch (s) {
            case "Approved": case "Cleared": case "Present": return 0xFF69F0AE;
            case "Rejected": case "Absent": return 0xFFFF8A80;
            default: return 0xFFFFD54F;
        }
    }
}
