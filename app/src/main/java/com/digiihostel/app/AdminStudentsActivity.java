package com.digiihostel.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.view.Gravity;
import android.widget.*;
import org.json.*;
import java.io.File;

/**
 * Admin > Student Details. Left bar: 1st-4th year (+ new student). Right: that year's students, each with Edit / Delete.
 * Top-right corner: "Requests" - registrations sent from the login screen, waiting for approval.
 */
public class AdminStudentsActivity extends BaseActivity {
    static final String REQ = "REQ";
    static final String[] ORD = {"1st", "2nd", "3rd", "4th"};
    String sel = "1";

    @Override void build() { splitLayout(104); }

    @Override protected void onResume() { super.onResume(); if (main != null) render(); }

    void render() { renderSide(); renderMain(); }

    JSONObject accountFor(JSONArray acc, String en) {
        for (int i = 0; i < acc.length(); i++) {
            JSONObject a = acc.optJSONObject(i);
            if (a.optString("role").equals("STUDENT") && a.optString("enrollment").equals(en)) return a;
        }
        return null;
    }

    static boolean knownYear(String y) { return y.equals("1") || y.equals("2") || y.equals("3") || y.equals("4"); }

    void renderSide() {
        side.removeAllViews();
        side.addView(text("Students", 18, true));
        JSONArray st = Store.list(this, "students");
        int[] n = new int[5];                       // 1..4, index 0 = other / unknown year
        for (int i = 0; i < st.length(); i++) {
            String y = st.optJSONObject(i).optString("year");
            n[knownYear(y) ? Integer.parseInt(y) : 0]++;
        }
        for (int y = 1; y <= 4; y++) {
            final String key = String.valueOf(y);
            side.addView(sideItem(ORD[y - 1] + " Year\n(" + n[y] + ")", sel.equals(key), v -> { sel = key; render(); }), lp(6));
        }
        if (n[0] > 0) side.addView(sideItem("Other\n(" + n[0] + ")", sel.equals("0"), v -> { sel = "0"; render(); }), lp(6));
        side.addView(button("+ New student", v -> startActivity(new Intent(this, RegisterActivity.class).putExtra("mode", RegisterActivity.CREATE))), lp(14));
    }

    void renderMain() {
        main.removeAllViews();
        int pending = Store.list(this, "requests").length();
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        boolean req = sel.equals(REQ);
        String title = req ? "Registration Requests" : sel.equals("0") ? "Other / unknown year" : ORD[Integer.parseInt(sel) - 1] + " Year Students";
        head.addView(text(title, 18, true), new LinearLayout.LayoutParams(0, -2, 1));
        Button rb = button(req ? "← Students" : "Requests (" + pending + ")", v -> { sel = req ? "1" : REQ; render(); });
        if (!req && pending > 0) rb.setBackground(box(0xCCFFA726, 0xFFFFD54F));
        head.addView(rb);                           // sits in the right-hand corner
        main.addView(head);
        if (req) renderRequests(); else renderStudents();
    }

    // ---------- students of one year ----------
    void renderStudents() {
        JSONArray st = Store.list(this, "students"), acc = Store.list(this, "accounts");
        int shown = 0;
        for (int i = 0; i < st.length(); i++) {
            final JSONObject s = st.optJSONObject(i);
            String y = s.optString("year");
            boolean match = sel.equals("0") ? !knownYear(y) : y.equals(sel);
            if (!match) continue;
            shown++;
            JSONObject a = accountFor(acc, s.optString("enrollment"));
            LinearLayout c = card();
            c.addView(text(s.optString("name") + "  (" + s.optString("enrollment") + ")", 16, true));
            c.addView(text(s.optString("school") + "\n" + s.optString("branch") + " - Year " + y, 13, false));
            c.addView(text(s.optString("hostel") + ", room " + s.optString("room") + "  |  Phone " + s.optString("phone"), 13, false));
            c.addView(text("Username: " + (a == null ? "-" : a.optString("username")) + "   Password: " + (a == null ? "-" : a.optString("password")), 13, false));
            LinearLayout bar = new LinearLayout(this);
            bar.addView(button("Edit", v -> editStudent(s)), half());
            Button del = button("Delete", v -> confirmDelete(s));
            del.setBackground(box(0x88FF5252, 0xFFFF8A80));
            bar.addView(del, half());
            c.addView(bar, lp(6));
            c.setOnClickListener(v -> editStudent(s));
            main.addView(c, lp(10));
        }
        if (shown == 0) main.addView(text("No students in this year yet.", 15, false), lp(12));
    }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(box(0x33FFFFFF, 0x6680D8FF));
        return c;
    }

    LinearLayout.LayoutParams half() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
        p.setMargins(dp(3), 0, dp(3), 0);
        return p;
    }

    void editStudent(JSONObject s) {
        startActivity(new Intent(this, RegisterActivity.class).putExtra("mode", RegisterActivity.EDIT).putExtra("en", s.optString("enrollment")));
    }

    void confirmDelete(final JSONObject s) {
        new AlertDialog.Builder(this).setTitle("Delete student?")
                .setMessage("Delete " + s.optString("name") + " (" + s.optString("enrollment") + ")?\n\nTheir login, photo, outpass, complaint and attendance records are removed too, and their bed is freed. This cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> { Store.deleteStudent(this, s.optString("enrollment")); toast("Student deleted"); render(); })
                .setNegativeButton("Cancel", null).show();
    }

    // ---------- registration requests ----------
    void renderRequests() {
        JSONArray rq = Store.list(this, "requests");
        for (int i = 0; i < rq.length(); i++) {
            final JSONObject r = rq.optJSONObject(i);
            LinearLayout c = card();
            c.addView(text(r.optString("name") + "  (" + r.optString("enrollment") + ")", 16, true));
            c.addView(text("Requested on " + r.optString("date"), 12, false));
            c.addView(text(r.optString("gender") + " | DOB " + r.optString("dob") + " | Blood " + r.optString("blood"), 13, false));
            c.addView(text(r.optString("school") + "\n" + r.optString("branch") + " - Year " + r.optString("year"), 13, false));
            c.addView(text("Phone " + r.optString("phone") + " | Parent " + r.optString("parent"), 13, false));
            c.addView(text(r.optString("email"), 13, false));
            c.addView(text("Hostel: " + r.optString("hostel") + (r.optString("room").isEmpty() ? "" : ", room " + r.optString("room")), 13, false));
            LinearLayout bar = new LinearLayout(this);
            Button ok = button("Approve", v -> approve(r));
            ok.setBackground(box(0x8869F0AE, 0xFF69F0AE));
            Button no = button("Reject", v -> confirmReject(r));
            no.setBackground(box(0x88FF5252, 0xFFFF8A80));
            bar.addView(ok, half());
            bar.addView(no, half());
            c.addView(bar, lp(6));
            main.addView(c, lp(10));
        }
        if (rq.length() == 0) main.addView(text("No pending requests.", 15, false), lp(12));
    }

    void removeRequest(String en) {
        JSONArray rq = Store.list(this, "requests"), keep = new JSONArray();
        for (int i = 0; i < rq.length(); i++) if (!rq.optJSONObject(i).optString("enrollment").equals(en)) keep.put(rq.optJSONObject(i));
        Store.save(this, "requests", keep);
    }

    void approve(JSONObject r) {
        String en = r.optString("enrollment");
        if (Store.student(this, en) != null) { toast("This enrollment is already registered - reject the request"); return; }
        JSONObject s = new JSONObject();
        for (String k : Store.F.get("students")) Store.put(s, k, r.optString(k));
        String[] cred = Store.createStudent(this, s);
        new File(getFilesDir(), "req_" + en + ".jpg").renameTo(new File(getFilesDir(), "photo_" + en + ".jpg"));
        removeRequest(en);
        render();
        new AlertDialog.Builder(this).setTitle("Request approved")
                .setMessage(s.optString("name") + " can now log in.\n\nUsername: " + cred[0] + "\nPassword: " + cred[1] + "\n\nShare these with the student (also visible in their student card).")
                .setPositiveButton("OK", null).show();
    }

    void confirmReject(final JSONObject r) {
        new AlertDialog.Builder(this).setTitle("Reject request?")
                .setMessage("Reject the request from " + r.optString("name") + "? It will be removed.")
                .setPositiveButton("Reject", (d, w) -> {
                    removeRequest(r.optString("enrollment"));
                    new File(getFilesDir(), "req_" + r.optString("enrollment") + ".jpg").delete();
                    render();
                }).setNegativeButton("Cancel", null).show();
    }
}
