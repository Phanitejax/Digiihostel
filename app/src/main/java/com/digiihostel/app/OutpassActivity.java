package com.digiihostel.app;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Student outpass form (calendar + time pickers, dropdowns) with live status of own requests. */
public class OutpassActivity extends BaseActivity {
    LinearLayout list;

    TextView picker(String hint) {
        TextView t = text(hint, 15, false);
        t.setPadding(dp(12), dp(12), dp(12), dp(12));
        t.setBackground(box(0x33FFFFFF, 0x66FFFFFF));
        t.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (dv, y, mo, d) ->
                    new TimePickerDialog(this, (tv, h, mi) -> {
                        t.setText(String.format(Locale.US, "%04d-%02d-%02d %02d:%02d", y, mo + 1, d, h, mi));
                        t.setTag("set");
                    }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show(),
                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
        return t;
    }

    @Override void build() {
        add(text("Outpass Registration", 22, true));
        JSONObject s = Store.student(this, Session.enroll());
        EditText name = edit("Full name"), enr = edit("Enrollment number");
        if (s != null) { name.setText(s.optString("name")); enr.setText(s.optString("enrollment")); }
        TextView out = picker("Tap: date & time to go OUT"), in = picker("Tap: date & time to come IN");
        Spinner type = spinner("Daypass", "Nightpass"), by = spinner("Father", "Mother", "NOC");
        add(name); add(enr); add(out); add(in);
        add(text("Pass type", 13, false)); add(type);
        add(text("Permission by", 13, false)); add(by);
        add(button("Submit outpass request", v -> {
            if (name.getText().toString().trim().isEmpty() || enr.getText().toString().trim().isEmpty() || out.getTag() == null || in.getTag() == null) {
                Toast.makeText(this, "Fill all fields and pick both dates", Toast.LENGTH_SHORT).show();
                return;
            }
            Store.add(this, "outpasses", Store.row("outpasses", Store.newId(), enr.getText().toString().trim(), name.getText().toString().trim(),
                    out.getText().toString(), in.getText().toString(), (String) type.getSelectedItem(), (String) by.getSelectedItem(), "Pending"));
            Toast.makeText(this, "Submitted - status: Pending", Toast.LENGTH_SHORT).show();
            refresh();
        }));
        add(text("My outpass status", 18, true));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        add(list);
        refresh();
    }

    void refresh() {
        list.removeAllViews();
        JSONArray a = Store.list(this, "outpasses");
        for (int i = a.length() - 1; i >= 0; i--) {
            JSONObject o = a.optJSONObject(i);
            if (!o.optString("enrollment").equals(Session.enroll())) continue;
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setPadding(dp(14), dp(10), dp(14), dp(10));
            c.setBackground(box(0x33FFFFFF, 0x6680D8FF));
            c.addView(text(o.optString("type") + " - by " + o.optString("permission"), 15, true));
            c.addView(text("Out: " + o.optString("out") + "\nIn:  " + o.optString("in"), 13, false));
            TextView st = text("Status: " + o.optString("status"), 15, true);
            st.setTextColor(statusColor(o.optString("status")));
            c.addView(st);
            list.addView(c, lp(8));
        }
    }
}
