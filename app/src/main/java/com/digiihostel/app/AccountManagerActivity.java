package com.digiihostel.app;

import android.app.AlertDialog;
import android.text.InputType;
import android.view.Gravity;
import android.widget.*;
import org.json.*;

/** Admin > Account Manager. Left bar: Admin / Warden / Food Incharge. Right: that account type's details (only the admin can edit them). */
public class AccountManagerActivity extends BaseActivity {
    String sel = "ADMIN";

    @Override void build() {
        splitLayout(104);
        render();
    }

    void render() { renderSide(); renderMain(); }

    void renderSide() {
        side.removeAllViews();
        side.addView(text("Accounts", 18, true));
        String[][] items = {{"ADMIN", "Admin"}, {"WARDEN", "Warden"}, {"FOOD", "Food Incharge"}};
        for (String[] it : items) {
            final String key = it[0];
            side.addView(sideItem(it[1], sel.equals(key), v -> { sel = key; render(); }), lp(6));
        }
    }

    void renderMain() {
        main.removeAllViews();
        if (sel.equals("ADMIN")) renderAdmin(); else renderStaff(sel);
    }

    LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(14), dp(12), dp(14), dp(12));
        c.setBackground(box(0x33FFFFFF, 0x6680D8FF));
        return c;
    }

    EditText plain(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        return e;
    }

    EditText dateBox(String hint, String cur, boolean future, int fallbackYear) {
        final EditText e = plain(hint, cur);
        e.setFocusable(false);
        e.setOnClickListener(v -> pickDate(e.getText().toString(), future, fallbackYear, d -> e.setText(d)));
        return e;
    }

    // ---------- Admin ----------
    JSONObject findAccount(JSONArray all, String role, String username) {
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o.optString("role").equals(role) && o.optString("username").equals(username)) return o;
        }
        return null;
    }

    void renderAdmin() {
        main.addView(text("Admin account", 18, true));
        JSONArray all = Store.list(this, "accounts");
        JSONObject admin = null;
        for (int i = 0; i < all.length() && admin == null; i++) if (all.optJSONObject(i).optString("role").equals("ADMIN")) admin = all.optJSONObject(i);
        if (admin == null) { main.addView(text("No admin account found.", 15, false), lp(12)); return; }
        final String oldUser = admin.optString("username");
        LinearLayout c = card();
        c.addView(text("Username: " + oldUser, 15, true));
        c.addView(text("Password: " + admin.optString("password"), 15, true));
        c.addView(button("Edit username & password", v -> adminDialog(oldUser)), lp(8));
        main.addView(c, lp(10));
    }

    void adminDialog(final String oldUser) {
        JSONArray all0 = Store.list(this, "accounts");
        JSONObject cur = findAccount(all0, "ADMIN", oldUser);
        if (cur == null) return;
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(20), dp(10), dp(20), 0);
        final EditText u = plain("Username (no spaces)", cur.optString("username")), p = plain("Password (no spaces)", cur.optString("password"));
        u.setSingleLine();
        noSpaces(u); noSpaces(p); eyeToggle(p, 0xFF555555);
        b.addView(u); b.addView(p);
        final AlertDialog dlg = new AlertDialog.Builder(this).setTitle("Edit admin login").setView(b)
                .setPositiveButton("Save", null).setNegativeButton("Cancel", null).create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nu = u.getText().toString(), np = p.getText().toString();
            if (nu.isEmpty() || np.isEmpty()) { toast("Username and password can't be empty"); return; }
            if (Store.hasSpace(nu) || Store.hasSpace(np)) { toast("Spaces are not allowed"); return; }
            JSONArray all = Store.list(this, "accounts");
            JSONObject t = findAccount(all, "ADMIN", oldUser);
            if (t == null) return;
            for (int i = 0; i < all.length(); i++) {
                JSONObject o = all.optJSONObject(i);
                if (o != t && o.optString("username").equals(nu)) { toast("Username already taken"); return; }
            }
            Store.put(t, "username", nu);
            Store.put(t, "password", np);
            Store.save(this, "accounts", all);
            if (Session.me != null && Session.me.optString("username").equals(oldUser)) { Store.put(Session.me, "username", nu); Store.put(Session.me, "password", np); }
            dlg.dismiss();
            toast("Admin login updated");
            render();
        });
    }

    // ---------- Warden / Food incharge ----------
    void renderStaff(final String role) {
        final String label = role.equals("WARDEN") ? "Warden" : "Food Incharge";
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.addView(text(label + " accounts", 18, true), new LinearLayout.LayoutParams(0, -2, 1));
        head.addView(button("+ Add account", v -> staffDialog(role, null)));
        main.addView(head);
        JSONArray all = Store.list(this, "accounts");
        int shown = 0;
        for (int i = 0; i < all.length(); i++) {
            final JSONObject o = all.optJSONObject(i);
            if (!o.optString("role").equals(role)) continue;
            shown++;
            LinearLayout c = card();
            String dob = o.optString("dob");
            c.addView(text(o.optString("name").isEmpty() ? "(no name)" : o.optString("name"), 16, true));
            c.addView(text("Age: " + (dob.isEmpty() ? "-" : Store.ageFrom(dob)) + "    Phone: " + dash(o.optString("phone")), 13, false));
            c.addView(text("DOB: " + dash(dob) + "    Joined: " + dash(o.optString("joining")), 13, false));
            c.addView(text("Username: " + o.optString("username") + "   Password: " + o.optString("password"), 13, false));
            c.addView(button("Edit", v -> staffDialog(role, o)), lp(6));
            c.setOnClickListener(v -> staffDialog(role, o));
            main.addView(c, lp(10));
        }
        if (shown == 0) main.addView(text("No " + label.toLowerCase() + " accounts yet. Tap \"+ Add account\".", 15, false), lp(12));
    }

    static String dash(String s) { return s.isEmpty() ? "-" : s; }

    void staffDialog(final String role, final JSONObject acc) {
        final boolean isNew = acc == null;
        final String oldUser = isNew ? null : acc.optString("username");
        final String label = role.equals("WARDEN") ? "warden" : "food incharge";
        LinearLayout b = new LinearLayout(this);
        b.setOrientation(LinearLayout.VERTICAL);
        b.setPadding(dp(20), dp(10), dp(20), 0);
        final EditText name = plain("Full name", isNew ? "" : acc.optString("name"));
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        final EditText phone = plain("Phone number (10 digits)", isNew ? "" : acc.optString("phone"));
        digitsOnly(phone, 10);
        final EditText dob = dateBox("Date of birth (tap to choose)", isNew ? "" : acc.optString("dob"), false, 1990);
        final EditText join = dateBox("Joining date (tap to choose)", isNew ? "" : acc.optString("joining"), true, 2024);
        final EditText u = plain("Username (no spaces)", isNew ? "" : acc.optString("username"));
        final EditText p = plain("Password (no spaces)", isNew ? "" : acc.optString("password"));
        u.setSingleLine();
        noSpaces(u); noSpaces(p); eyeToggle(p, 0xFF555555);
        b.addView(name); b.addView(phone); b.addView(dob); b.addView(join); b.addView(u); b.addView(p);
        ScrollView sv = new ScrollView(this);
        sv.addView(b);
        AlertDialog.Builder bl = new AlertDialog.Builder(this).setTitle(isNew ? "Add " + label : "Edit " + label).setView(sv)
                .setPositiveButton("Save", null).setNegativeButton("Cancel", null);
        if (!isNew) bl.setNeutralButton("Delete", null);
        final AlertDialog dlg = bl.create();
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String un = u.getText().toString(), pw = p.getText().toString();
            if (name.getText().toString().trim().isEmpty()) { toast("Enter the name"); return; }
            if (phone.getText().toString().length() != 10) { toast("Phone number must be exactly 10 digits"); return; }
            if (dob.getText().toString().isEmpty()) { toast("Choose the date of birth"); return; }
            if (join.getText().toString().isEmpty()) { toast("Choose the joining date"); return; }
            if (un.isEmpty() || pw.isEmpty()) { toast("Username and password can't be empty"); return; }
            if (Store.hasSpace(un) || Store.hasSpace(pw)) { toast("Spaces are not allowed in username or password"); return; }
            JSONArray all = Store.list(this, "accounts");
            JSONObject t = isNew ? null : findAccount(all, role, oldUser);
            if (!isNew && t == null) { dlg.dismiss(); return; }
            for (int i = 0; i < all.length(); i++) {
                JSONObject o = all.optJSONObject(i);
                if (o != t && o.optString("username").equals(un)) { toast("Username already taken"); return; }
            }
            if (isNew) { t = Store.row("accounts", un, pw, role, "-"); all.put(t); }
            Store.put(t, "username", un);
            Store.put(t, "password", pw);
            Store.put(t, "name", name.getText().toString().trim());
            Store.put(t, "phone", phone.getText().toString());
            Store.put(t, "dob", dob.getText().toString());
            Store.put(t, "joining", join.getText().toString());
            Store.save(this, "accounts", all);
            dlg.dismiss();
            toast(isNew ? "Account created" : "Account updated");
            render();
        });
        if (!isNew) dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                new AlertDialog.Builder(this).setTitle("Delete account?")
                        .setMessage("Delete the " + label + " account \"" + oldUser + "\"? This cannot be undone.")
                        .setPositiveButton("Delete", (d, w) -> {
                            JSONArray all = Store.list(this, "accounts"), keep = new JSONArray();
                            for (int i = 0; i < all.length(); i++) {
                                JSONObject o = all.optJSONObject(i);
                                if (!(o.optString("role").equals(role) && o.optString("username").equals(oldUser))) keep.put(o);
                            }
                            Store.save(this, "accounts", keep);
                            dlg.dismiss();
                            render();
                        }).setNegativeButton("Cancel", null).show());
    }
}
