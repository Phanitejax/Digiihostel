package com.digiihostel.app;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import android.widget.*;
import org.json.JSONObject;
import java.io.File;

public class IdCardActivity extends BaseActivity {
    @Override void build() {
        add(text("Student ID Card", 22, true));
        final JSONObject s = Store.student(this, Session.enroll());
        if (s == null) { add(text("No student profile found.", 16, false)); return; }
        final Bitmap photo = BitmapFactory.decodeFile(new File(getFilesDir(), "photo_" + s.optString("enrollment") + ".jpg").getAbsolutePath());
        View v = new View(this) {
            @Override protected void onDraw(Canvas c) {
                float w = getWidth(), h = getHeight(), d = getResources().getDisplayMetrics().density;
                Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(0xEE0A2A66); c.drawRoundRect(0, 0, w, h, 20 * d, 20 * d, p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(2 * d); p.setColor(0xFF4FC3F7);
                c.drawRoundRect(2 * d, 2 * d, w - 2 * d, h - 2 * d, 20 * d, 20 * d, p);
                p.setStyle(Paint.Style.FILL); p.setTextSize(20 * d); p.setTypeface(Typeface.DEFAULT_BOLD);
                c.drawText("DIGIIHOSTEL - STUDENT ID", 18 * d, 36 * d, p);
                RectF box = new RectF(18 * d, 56 * d, 138 * d, 206 * d);
                if (photo != null) c.drawBitmap(photo, null, box, null);
                else { p.setStyle(Paint.Style.STROKE); c.drawRect(box, p); p.setStyle(Paint.Style.FILL); p.setTextSize(14 * d); c.drawText("PHOTO", 56 * d, 135 * d, p); }
                p.setColor(Color.WHITE); p.setTypeface(Typeface.DEFAULT); p.setTextSize(15 * d);
                String[] k = {"Name", "Enrollment", "Branch", "School", "Parent Contact"};
                String[] val = {s.optString("name"), s.optString("enrollment"), s.optString("branch"), s.optString("school"), s.optString("parent")};
                for (int i = 0; i < k.length; i++) {
                    p.setColor(0xFF4FC3F7); p.setTextSize(11 * d); c.drawText(k[i], 156 * d, (76 + i * 30) * d, p);
                    p.setColor(Color.WHITE); p.setTextSize(15 * d);
                    float maxW = w - 156 * d - 10 * d;
                    while (p.measureText(val[i]) > maxW && p.getTextSize() > 7 * d) p.setTextSize(p.getTextSize() - 0.5f * d);   // long school / branch names shrink to fit
                    c.drawText(val[i], 156 * d, (92 + i * 30) * d, p);
                }
            }
        };
        root.addView(v, new LinearLayout.LayoutParams(-1, dp(240)));
        add(text("My details", 18, true));
        String[] lab = {"Enrollment No", "Name", "Gender", "Date of birth", "Branch", "Year", "School", "Phone", "E-mail",
                "Parent contact", "Hostel name", "Room no", "Blood group"};
        String[] keys = Store.F.get("students");
        LinearLayout det = new LinearLayout(this);
        det.setOrientation(LinearLayout.VERTICAL);
        det.setPadding(dp(14), dp(10), dp(14), dp(10));
        det.setBackground(box(0x33FFFFFF, 0x6680D8FF));
        for (int i = 0; i < keys.length; i++) {
            LinearLayout row = new LinearLayout(this);
            TextView l = text(lab[i], 13, false);
            l.setTextColor(0xFF4FC3F7);
            row.addView(l, new LinearLayout.LayoutParams(0, -2, 0.42f));
            row.addView(text(s.optString(keys[i]), 15, false), new LinearLayout.LayoutParams(0, -2, 0.58f));
            row.setPadding(0, dp(5), 0, dp(5));
            det.addView(row);
        }
        add(det);
    }
}
