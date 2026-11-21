package com.digiihostel.app;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.util.*;

/** Hostel names in a left side bar; rooms + occupancy for the selected hostel in the main area. */
public class HostelsActivity extends BaseActivity {
    boolean edit;
    String sel;
    LinearLayout side, main;

    @Override void build() {
        edit = getIntent().getBooleanExtra("edit", false) && Session.role().equals("WARDEN");
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
        h.addView(ss, new LinearLayout.LayoutParams(dp(112), -1));
        h.addView(ms, new LinearLayout.LayoutParams(0, -1, 1));
        f.addView(h);
        setContentView(f);
        JSONArray hs = Store.list(this, "hostels");
        if (hs.length() > 0) sel = hs.optJSONObject(0).optString("name");
        renderSide();
        renderMain();
    }

    void renderSide() {
        side.removeAllViews();
        side.addView(text("Hostels", 18, true));
        JSONArray hs = Store.list(this, "hostels");
        for (int i = 0; i < hs.length(); i++) {
            final String name = hs.optJSONObject(i).optString("name");
            String g = hs.optJSONObject(i).optString("gender");
            TextView t = text((i + 1) + ". " + name + (g.isEmpty() ? "" : "\n(" + g + ")"), 14, true);
            t.setPadding(dp(8), dp(12), dp(8), dp(12));
            t.setBackground(box(name.equals(sel) ? 0xAA4FC3F7 : 0x33FFFFFF, 0x6680D8FF));
            t.setOnClickListener(v -> { sel = name; renderSide(); renderMain(); });
            if (edit) t.setOnLongClickListener(v -> { hostelDialog(name); return true; });
            side.addView(t, lp(6));
        }
        if (edit) {
            side.addView(button("+ Hostel", v -> hostelDialog(null)), lp(10));
            TextView hint = text("Long-press a hostel to rename or delete", 11, false);
            side.addView(hint, lp(6));
        }
    }

    void renderMain() {
        main.removeAllViews();
        if (sel == null) { main.addView(text("No hostels yet.", 16, false)); return; }
        JSONArray hs = Store.list(this, "hostels");
        String ranges = "";
        for (int i = 0; i < hs.length(); i++) if (hs.optJSONObject(i).optString("name").equals(sel)) ranges = hs.optJSONObject(i).optString("ranges");
        JSONArray all = Store.list(this, "rooms");
        List<Integer> idxs = new ArrayList<>();
        int beds = 0, occ = 0;
        for (int i = 0; i < all.length(); i++) {
            JSONObject r = all.optJSONObject(i);
            if (r.optString("hostel").equals(sel)) { idxs.add(i); beds += r.optInt("beds"); occ += r.optInt("occupied"); }
        }
        final JSONArray fa = all;
        Collections.sort(idxs, (a, b) -> Integer.compare(num(fa.optJSONObject(a).optString("room")), num(fa.optJSONObject(b).optString("room"))));
        main.addView(text(sel, 22, true));
        String hg = "";
        for (int i = 0; i < hs.length(); i++) if (hs.optJSONObject(i).optString("name").equals(sel)) hg = hs.optJSONObject(i).optString("gender");
        TextView who = text(hg.equals("Boys") ? "Boys' hostel - only male students can book rooms"
                : hg.equals("Girls") ? "Girls' hostel - only female students can book rooms" : "Booking gender not set - open to all", 13, true);
        who.setTextColor(hg.equals("Girls") ? 0xFFF48FB1 : hg.equals("Boys") ? 0xFF81D4FA : 0xFFFFD54F);
        main.addView(who);
        main.addView(text("Room nos. " + ranges, 13, false));
        main.addView(text("Students / Total beds:  " + occ + " / " + beds + "   (" + idxs.size() + " rooms)", 14, true));
        if (edit) main.addView(button("+ Add room", v -> roomDialog(-1)), lp(8));
        int lastGroup = Integer.MIN_VALUE;
        LinearLayout row = null;
        int inRow = 0;
        for (int idx : idxs) {
            final int ix = idx;
            JSONObject r = all.optJSONObject(idx);
            int rn = num(r.optString("room")), g = rn / 100;
            if (g != lastGroup) {
                lastGroup = g;
                TextView gh = text("Rooms " + g + "01 - " + g + "20", 14, true);
                gh.setTextColor(0xFF4FC3F7);
                main.addView(gh, lp(14));
                row = null;
            }
            if (row == null || inRow == 3) { row = new LinearLayout(this); main.addView(row, lp(6)); inRow = 0; }
            int o = r.optInt("occupied"), b = r.optInt("beds");
            int color = o >= b ? 0xAAFF5252 : (o == 0 ? 0xAA2E7D32 : 0xAAF9A825);
            TextView t = text(r.optString("room") + "\n" + o + "/" + b, 14, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(2), dp(8), dp(2), dp(8));
            t.setBackground(box(color, 0x88FFFFFF));
            t.setOnClickListener(v -> {
                if (edit) roomDialog(ix);
                else Toast.makeText(this, "Room " + r.optString("room") + ": " + r.optString("occupied") + " of " + r.optString("beds") + " beds filled", Toast.LENGTH_SHORT).show();
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
            p.setMargins(dp(3), 0, dp(3), 0);
            row.addView(t, p);
            inRow++;
        }
        TextView legend = text("Green = vacant, yellow = partly filled, red = full", 11, false);
        main.addView(legend, lp(14));
    }

    static int num(String s) { try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; } }

    void roomDialog(final int idx) {
        final JSONArray arr = Store.list(this, "rooms");
        final JSONObject r = idx >= 0 ? arr.optJSONObject(idx) : Store.row("rooms", sel, "", "3", "0");
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(20), dp(10), dp(20), 0);
        final EditText rn = new EditText(this), bd = new EditText(this), oc = new EditText(this);
        rn.setHint("Room number"); bd.setHint("Total beds"); oc.setHint("Students now in room");
        rn.setText(r.optString("room")); bd.setText(r.optString("beds")); oc.setText(r.optString("occupied"));
        b.addView(rn); b.addView(bd); b.addView(oc);
        AlertDialog.Builder bl = new AlertDialog.Builder(this).setTitle(sel + (idx >= 0 ? " - edit room" : " - add room")).setView(b)
                .setPositiveButton("Save", (d, w) -> {
                    int beds = Math.max(0, num(bd.getText().toString()));
                    int occ = Math.min(beds, Math.max(0, num(oc.getText().toString())));
                    Store.put(r, "room", rn.getText().toString().trim());
                    Store.put(r, "beds", String.valueOf(beds));
                    Store.put(r, "occupied", String.valueOf(occ));
                    if (idx < 0) arr.put(r);
                    Store.save(this, "rooms", arr);
                    renderMain();
                }).setNegativeButton("Cancel", null);
        if (idx >= 0) bl.setNeutralButton("Delete room", (d, w) -> { arr.remove(idx); Store.save(this, "rooms", arr); renderMain(); });
        bl.show();
    }

    /** old == null: add a new hostel. Otherwise rename or delete it. */
    void hostelDialog(final String old) {
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(20), dp(10), dp(20), 0);
        final EditText nm = new EditText(this), rg = new EditText(this);
        nm.setHint("Hostel name"); rg.setHint("Room number ranges (info text)");
        final JSONArray hs = Store.list(this, "hostels");
        int at = -1;
        for (int i = 0; i < hs.length(); i++) if (old != null && hs.optJSONObject(i).optString("name").equals(old)) at = i;
        final int pos = at;
        if (pos >= 0) { nm.setText(old); rg.setText(hs.optJSONObject(pos).optString("ranges")); }
        final Spinner gd = spinner("Boys", "Girls");
        TextView gl = new TextView(this);
        gl.setText("Who can book rooms in this hostel?");
        if (pos >= 0) gd.setSelection(hs.optJSONObject(pos).optString("gender").equals("Girls") ? 1 : 0);
        b.addView(nm); b.addView(rg); b.addView(gl); b.addView(gd);
        AlertDialog.Builder bl = new AlertDialog.Builder(this).setTitle(old == null ? "Add hostel" : "Edit hostel").setView(b)
                .setPositiveButton("Save", (d, w) -> {
                    String n = nm.getText().toString().trim();
                    if (n.isEmpty()) return;
                    String gender = String.valueOf(gd.getSelectedItem());
                    if (pos < 0) hs.put(Store.row("hostels", n, rg.getText().toString().trim(), gender));
                    else {
                        JSONObject h = hs.optJSONObject(pos);
                        Store.put(h, "name", n);
                        Store.put(h, "ranges", rg.getText().toString().trim());
                        Store.put(h, "gender", gender);
                        for (String col : new String[]{"rooms", "students"}) {
                            JSONArray a = Store.list(this, col);
                            for (int i = 0; i < a.length(); i++) {
                                JSONObject o = a.optJSONObject(i);
                                String key = col.equals("rooms") ? "hostel" : "hostel";
                                if (o.optString(key).equals(old)) Store.put(o, key, n);
                            }
                            Store.save(this, col, a);
                        }
                    }
                    Store.save(this, "hostels", hs);
                    sel = n;
                    renderSide(); renderMain();
                }).setNegativeButton("Cancel", null);
        if (pos >= 0) bl.setNeutralButton("Delete hostel", (d, w) -> {
            hs.remove(pos);
            Store.save(this, "hostels", hs);
            JSONArray a = Store.list(this, "rooms"), keep = new JSONArray();
            for (int i = 0; i < a.length(); i++) if (!a.optJSONObject(i).optString("hostel").equals(old)) keep.put(a.optJSONObject(i));
            Store.save(this, "rooms", keep);
            sel = hs.length() > 0 ? hs.optJSONObject(0).optString("name") : null;
            renderSide(); renderMain();
        });
        bl.show();
    }
}
