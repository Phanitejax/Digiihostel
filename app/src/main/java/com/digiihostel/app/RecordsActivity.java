package com.digiihostel.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.*;
import org.json.*;
import java.util.*;

/** One generic screen for every list: notices, food, contacts, rooms, attendance, complaints, outpasses, students. */
public class RecordsActivity extends BaseActivity {
    String col, flags, fKey, fVal;
    String[] actions, hide;
    LinearLayout list;

    boolean has(String s) { return flags.contains("," + s + ","); }
    boolean hidden(String k) { for (String h : hide) if (h.equals(k)) return true; return false; }

    @Override void build() {
        Intent in = getIntent();
        col = in.getStringExtra("col");
        flags = "," + in.getStringExtra("flags") + ",";
        String f = in.getStringExtra("filter");
        if (f != null && f.contains("=")) { fKey = f.split("=")[0]; fVal = f.split("=", 2)[1]; }
        String a = in.getStringExtra("actions");
        actions = (a == null || a.isEmpty()) ? new String[0] : a.split(";");
        String h = in.getStringExtra("hide");
        hide = h == null ? new String[0] : h.split(",");
        add(text(in.getStringExtra("title"), 22, true));
        if (has("add")) add(button("+ Add", v -> editor(-1)));
        if (has("bulk")) add(button("Mark all students present today", v -> {
            JSONArray st = Store.list(this, "students");
            for (int i = 0; i < st.length(); i++) {
                JSONObject s = st.optJSONObject(i);
                Store.add(this, "attendance", Store.row("attendance", Store.today(), s.optString("enrollment"), s.optString("name"), "Present"));
            }
            render();
        }));
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        add(list);
        render();
    }

    void render() {
        list.removeAllViews();
        JSONArray arr = Store.list(this, col);
        String[] keys = Store.F.get(col);
        int shown = 0;
        for (int i = 0; i < arr.length(); i++) {
            final int idx = i;
            final JSONObject r = arr.optJSONObject(i);
            if (fKey != null && !fVal.equals(r.optString(fKey))) continue;
            shown++;
            LinearLayout c = new LinearLayout(this);
            c.setOrientation(LinearLayout.VERTICAL);
            c.setPadding(dp(14), dp(12), dp(14), dp(12));
            c.setBackground(box(0x33FFFFFF, 0x6680D8FF));
            if (col.equals("rooms")) {
                c.addView(text(r.optString("hostel") + " - Room " + r.optString("room"), 16, true));
                c.addView(text("Students / Total beds:  " + r.optString("occupied") + " / " + r.optString("beds"), 14, false));
            }
            if (!col.equals("rooms") || has("edit")) {
                for (String k : keys) {
                    if (k.equals("id") || (col.equals("rooms") && !has("edit"))) continue;
                    TextView t = text(cap(k) + ": " + r.optString(k), 14, false);
                    if (k.equals("status")) t.setTextColor(statusColor(r.optString(k)));
                    c.addView(t);
                }
            }
            LinearLayout bar = new LinearLayout(this);
            for (String act : actions) {
                String[] p = act.split("\\|");
                bar.addView(button(p[0], v -> {
                    Store.put(r, p[1], p[2]);
                    Store.save(this, col, arr);
                    render();
                }));
            }
            if (has("del")) bar.addView(button("Delete", v -> { arr.remove(idx); Store.save(this, col, arr); render(); }));
            if (bar.getChildCount() > 0) c.addView(bar);
            if (has("edit")) c.setOnClickListener(v -> editor(idx));
            list.addView(c, lp(10));
        }
        if (shown == 0) list.addView(text("Nothing here yet.", 15, false));
    }

    JSONObject fresh() {
        JSONObject r = new JSONObject();
        for (String k : Store.F.get(col)) {
            String v = "";
            switch (k) {
                case "id": v = Store.newId(); break;
                case "date": v = Store.today(); break;
                case "by": v = Session.role(); break;
                case "enrollment": v = Session.enroll().equals("-") ? "" : Session.enroll(); break;
                case "status": v = col.equals("attendance") ? "Present" : "Pending"; break;
            }
            Store.put(r, k, v);
        }
        return r;
    }

    void editor(int idx) {
        final JSONArray arr = Store.list(this, col);
        final JSONObject r = idx >= 0 ? arr.optJSONObject(idx) : fresh();
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(10), dp(20), 0);
        final Map<String, EditText> ed = new LinkedHashMap<>();
        for (String k : Store.F.get(col)) {
            if (k.equals("id") || hidden(k)) continue;
            EditText e = new EditText(this);
            e.setHint(cap(k));
            e.setText(r.optString(k));
            box.addView(e);
            ed.put(k, e);
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(box);
        new AlertDialog.Builder(this).setTitle(idx >= 0 ? "Edit" : "Add").setView(sv)
                .setPositiveButton("Save", (d, w) -> {
                    for (Map.Entry<String, EditText> en : ed.entrySet()) Store.put(r, en.getKey(), en.getValue().getText().toString().trim());
                    if (idx < 0) arr.put(r);
                    Store.save(this, col, arr);
                    render();
                }).setNegativeButton("Cancel", null).show();
    }
}
