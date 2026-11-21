package com.digiihostel.app;

import android.content.Intent;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.*;
import java.util.*;

public class DashboardActivity extends BaseActivity {
    Intent L(String title, String col, String flags, String filter, String actions, String hide) {
        return new Intent(this, RecordsActivity.class).putExtra("title", title).putExtra("col", col).putExtra("flags", flags)
                .putExtra("filter", filter).putExtra("actions", actions).putExtra("hide", hide);
    }
    Intent A(Class<?> c) { return new Intent(this, c); }

    @Override void build() {
        if (Session.me == null) { startActivity(A(LoginActivity.class)); finish(); return; }
        String role = Session.role(), en = Session.enroll(), title;
        String clear = "Mark Cleared|status|Cleared";
        LinkedHashMap<String, Intent> m = new LinkedHashMap<>();
        switch (role) {
            case "STUDENT":
                title = "STUDENT DASHBOARD";
                m.put("🪪 ID Card", A(IdCardActivity.class));
                m.put("📢 Notices (Warden)", L("Notices", "notices", "", null, null, null));
                m.put("🍽 Weekly Food Timetable", A(FoodActivity.class));
                m.put("📝 Outpass Register", A(OutpassActivity.class));
                m.put("🛏 Room Register", A(HostelsActivity.class));
                m.put("✅ Hostel Attendance", L("My Attendance", "attendance", "", "enrollment=" + en, null, null));
                m.put("📮 Complaint Box", L("Complaint Box", "complaints", "add", "enrollment=" + en, null, "enrollment,status"));
                m.put("📞 Contacts", L("Contacts", "contacts", "", null, null, null));
                break;
            case "WARDEN":
                title = "WARDEN DASHBOARD";
                m.put("📢 Notices (Chat)", L("Notices", "notices", "add,edit,del", null, null, null));
                m.put("📝 Outpass Approval / Rejection", L("Outpass Requests", "outpasses", "", null, "Approve|status|Approved;Reject|status|Rejected", null));
                m.put("🛏 Room Register (Edit)", A(HostelsActivity.class).putExtra("edit", true));
                m.put("✅ Attendance Records", L("Attendance", "attendance", "add,edit,del,bulk", null, null, null));
                m.put("📮 Complaints (View & Clear)", L("Complaints", "complaints", "", null, clear, null));
                m.put("🍽 Food Timetable (View)", A(FoodActivity.class));
                m.put("📞 Contacts Manager", L("Contacts", "contacts", "add,edit,del", null, null, null));
                break;
            case "FOOD":
                title = "FOOD INCHARGE DASHBOARD";
                m.put("🍽 Food Timetable (Edit)", A(FoodActivity.class).putExtra("edit", true));
                m.put("📮 Complaint Box (Rectification)", L("Complaints", "complaints", "", null, clear, null));
                m.put("📞 Contacts Manager", L("Contacts", "contacts", "add,edit,del", null, null, null));
                break;
            default:
                title = "ADMIN DASHBOARD";
                m.put("🎓 Student Details", A(AdminStudentsActivity.class));
                m.put("👥 Account Manager", A(AccountManagerActivity.class));
                m.put("🛏 Room Registers", A(HostelsActivity.class));
                m.put("📮 Complaint Box & Clearance", L("Complaints", "complaints", "", null, clear, null));
                m.put("🍽 Food Timetable Management", A(FoodActivity.class).putExtra("edit", true));
        }
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(title, 18, true), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(button("Logout", v -> { Session.me = null; startActivity(A(LoginActivity.class)); finish(); }));
        add(head);
        add(text("Signed in as " + Session.me.optString("username"), 13, false));
        LinearLayout row = null;
        int i = 0;
        for (Map.Entry<String, Intent> e : m.entrySet()) {
            if (i++ % 2 == 0) { row = new LinearLayout(this); root.addView(row, lp(12)); }
            TextView t = text(e.getKey(), 15, true);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(8), dp(8), dp(8), dp(8));
            t.setBackground(box(0x442196F3, 0xFF4FC3F7));
            t.setOnClickListener(v -> startActivity(e.getValue()));
            t.setOnTouchListener((v, ev) -> {                    // little press-in effect
                if (ev.getAction() == MotionEvent.ACTION_DOWN) v.animate().scaleX(0.94f).scaleY(0.94f).setDuration(90).start();
                else if (ev.getAction() == MotionEvent.ACTION_UP || ev.getAction() == MotionEvent.ACTION_CANCEL) v.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                return false;
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(100), 1);
            p.setMargins(dp(4), 0, dp(4), 0);
            row.addView(t, p);
        }
    }
}
