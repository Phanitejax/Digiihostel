package com.digiihostel.app;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.text.InputType;
import android.view.Gravity;
import android.view.animation.OvershootInterpolator;
import android.widget.*;
import org.json.*;

public class LoginActivity extends BaseActivity {
    @Override void build() {
        root.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo = text("🏠", 56, false);
        logo.setGravity(Gravity.CENTER);
        logo.setTag("noCascade");
        add(logo);
        TextView t = text("Digiihostel", 38, true);
        t.setGravity(Gravity.CENTER); t.setTextColor(0xFF4FC3F7);
        t.setShadowLayer(dp(14), 0, 0, 0xFF4FC3F7);
        add(t);
        TextView sub = text("College Hostel Management System", 14, false);
        sub.setGravity(Gravity.CENTER);
        add(sub);
        EditText u = edit("Username"), p = edit("Password");
        u.setSingleLine();
        eyeToggle(p, 0xFFFFFFFF);
        noSpaces(u); noSpaces(p);
        add(u); add(p);
        add(button("Login", v -> {
            String un = u.getText().toString(), pw = p.getText().toString();
            if (Store.hasSpace(un) || Store.hasSpace(pw)) { toast("Invalid - spaces are not allowed in username or password"); return; }
            JSONArray a = Store.list(this, "accounts");
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o.optString("username").equals(un) && o.optString("password").equals(pw)) {
                    Session.me = o;
                    startActivity(new Intent(this, DashboardActivity.class));
                    finish();
                    return;
                }
            }
            toast("Invalid username or password");
        }));
        add(button("Register (new student)", v -> startActivity(new Intent(this, RegisterActivity.class))));

        // special opening animation: logo spins in with a bounce, then keeps a gentle heartbeat; title glows
        logo.setScaleX(0f); logo.setScaleY(0f); logo.setRotation(-360f);
        logo.animate().scaleX(1f).scaleY(1f).rotation(0f).setStartDelay(250).setDuration(1100)
                .setInterpolator(new OvershootInterpolator(2.2f)).withEndAction(() -> {
                    ObjectAnimator sx = ObjectAnimator.ofFloat(logo, "scaleX", 1f, 1.12f);
                    ObjectAnimator sy = ObjectAnimator.ofFloat(logo, "scaleY", 1f, 1.12f);
                    for (ObjectAnimator o : new ObjectAnimator[]{sx, sy}) {
                        o.setDuration(1100); o.setRepeatMode(ValueAnimator.REVERSE); o.setRepeatCount(ValueAnimator.INFINITE); o.start();
                    }
                }).start();
        ObjectAnimator glow = ObjectAnimator.ofFloat(t, "alpha", 0.75f, 1f);
        glow.setDuration(1600); glow.setRepeatMode(ValueAnimator.REVERSE); glow.setRepeatCount(ValueAnimator.INFINITE);
        glow.setStartDelay(1500);
        glow.start();
    }
}
