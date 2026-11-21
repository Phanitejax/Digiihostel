package com.digiihostel.app;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.text.SimpleDateFormat;
import java.util.*;

/** Tiny JSON-in-SharedPreferences database. One JSON array per collection. */
class Store {
    static final Map<String, String[]> F = new HashMap<>();
    static {
        F.put("accounts", new String[]{"username", "password", "role", "enrollment", "name", "phone", "dob", "joining"});
        F.put("students", new String[]{"enrollment", "name", "gender", "dob", "branch", "year", "school", "phone", "email", "parent", "hostel", "room", "blood"});
        F.put("outpasses", new String[]{"id", "enrollment", "name", "out", "in", "type", "permission", "status"});
        F.put("complaints", new String[]{"id", "enrollment", "complaint", "status"});
        F.put("notices", new String[]{"date", "notice", "by"});
        F.put("food", new String[]{"day", "breakfast", "lunch", "snacks", "dinner"});
        F.put("contacts", new String[]{"name", "phone"});
        F.put("rooms", new String[]{"hostel", "room", "beds", "occupied"});
        F.put("attendance", new String[]{"date", "enrollment", "name", "status"});
        F.put("hostels", new String[]{"name", "ranges", "gender"});
        F.put("requests", new String[]{"enrollment", "name", "gender", "dob", "branch", "year", "school", "phone", "email", "parent", "hostel", "room", "blood", "date"});
    }

    static SharedPreferences sp(Context c) { return c.getSharedPreferences("digiihostel", Context.MODE_PRIVATE); }

    static JSONArray list(Context c, String n) {
        try { return new JSONArray(sp(c).getString(n, "[]")); } catch (JSONException e) { return new JSONArray(); }
    }
    static void save(Context c, String n, JSONArray a) { sp(c).edit().putString(n, a.toString()).apply(); }
    static void add(Context c, String n, JSONObject o) { JSONArray a = list(c, n); a.put(o); save(c, n, a); }
    static void put(JSONObject o, String k, String v) { try { o.put(k, v); } catch (JSONException ignored) { } }

    static JSONObject row(String col, String... vals) {
        String[] k = F.get(col);
        JSONObject j = new JSONObject();
        for (int i = 0; i < k.length && i < vals.length; i++) put(j, k[i], vals[i]);
        return j;
    }
    static String today() { return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date()); }
    static String newId() { return String.valueOf(System.currentTimeMillis() % 1000000); }

    static JSONObject student(Context c, String en) {
        JSONArray a = list(c, "students");
        for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i).optString("enrollment").equals(en)) return a.optJSONObject(i);
        return null;
    }

    static JSONArray arr(String... v) { JSONArray a = new JSONArray(); for (String x : v) a.put(x); return a; }

    /** Creates the 4 test accounts + sample data on first launch, then the hostel/menu data (v2). */
    static void seed(Context c) {
        if (!sp(c).getBoolean("seeded", false)) {
            add(c, "accounts", row("accounts", "admin", "admin123", "ADMIN", "-"));
            add(c, "accounts", row("accounts", "student", "student123", "STUDENT", "202301010001"));
            add(c, "accounts", row("accounts", "warden", "warden123", "WARDEN", "-", "Default Warden", "9000000001", "1985-06-10", "2020-07-01"));
            add(c, "accounts", row("accounts", "food", "food123", "FOOD", "-", "Default Food Incharge", "9000000002", "1988-03-22", "2021-01-15"));
            add(c, "students", row("students", "202301010001", "Test Student", "Male", "2005-01-15", "B.Tech Computer Science and Engineering", "2",
                    "School of Engineering & Sciences(SOES)", "9876543210", "student@test.com", "9123456780", "Apollo", "101", "B+"));
            add(c, "contacts", row("contacts", "Warden", "9000000001"));
            add(c, "contacts", row("contacts", "Mess Incharge", "9000000002"));
            add(c, "contacts", row("contacts", "Security Gate", "9000000003"));
            add(c, "notices", row("notices", today(), "Welcome to Digiihostel. Hostel gate closes at 9 PM.", "WARDEN"));
            sp(c).edit().putBoolean("seeded", true).apply();
        }
        if (!sp(c).getBoolean("seed2", false)) {
            String[] names = {"Apollo", "Zeus", "Centurus", "Athena 2", "Athena 1", "Sofia", "Herculus"};
            int[] base = {0, 0, 3000, 2000, 1000, 0, 0};
            JSONArray hs = new JSONArray(), rooms = new JSONArray();
            for (int i = 0; i < names.length; i++) {
                StringBuilder rg = new StringBuilder();
                for (int f = 1; f <= 4; f++) {
                    int s = base[i] + f * 100 + 1;
                    if (f > 1) rg.append(",");
                    rg.append(s).append("-").append(s + 19);
                    for (int n = 0; n < 20; n++)
                        rooms.put(row("rooms", names[i], String.valueOf(s + n), "3", (i == 0 && f == 1 && n == 0) ? "1" : "0"));
                }
                hs.put(row("hostels", names[i], rg.toString()));
            }
            save(c, "hostels", hs);
            save(c, "rooms", rooms);
            JSONArray st = list(c, "students");
            for (int i = 0; i < st.length(); i++) {
                JSONObject o = st.optJSONObject(i);
                if (o.optString("hostel").equals("Hostel A")) put(o, "hostel", "Apollo");
            }
            save(c, "students", st);
            JSONArray menu = new JSONArray();
            for (String d : new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"}) {
                JSONObject m = new JSONObject();
                try {
                    m.put("day", d);
                    m.put("breakfast", arr("Poha", "Boiled Egg", "Tea"));
                    m.put("lunch", arr("Dal", "Rice", "Roti", "Mixed Veg"));
                    m.put("snacks", arr("Samosa"));
                    m.put("dinner", arr("Roti", "Paneer", "Dal", "Rice"));
                    m.put("dessert", arr("Gulab Jamun"));
                } catch (JSONException ignored) { }
                menu.put(m);
            }
            save(c, "menu", menu);
            sp(c).edit().putBoolean("seed2", true).apply();
        }

        // v3: staff details for the default warden / food accounts of older installs
        if (!sp(c).getBoolean("seed3", false)) {
            JSONArray acc = list(c, "accounts");
            for (int i = 0; i < acc.length(); i++) {
                JSONObject o = acc.optJSONObject(i);
                if (o.optString("role").equals("WARDEN") && o.optString("name").isEmpty() && o.optString("username").equals("warden")) {
                    put(o, "name", "Default Warden"); put(o, "phone", "9000000001"); put(o, "dob", "1985-06-10"); put(o, "joining", "2020-07-01");
                }
                if (o.optString("role").equals("FOOD") && o.optString("name").isEmpty() && o.optString("username").equals("food")) {
                    put(o, "name", "Default Food Incharge"); put(o, "phone", "9000000002"); put(o, "dob", "1988-03-22"); put(o, "joining", "2021-01-15");
                }
            }
            save(c, "accounts", acc);
            sp(c).edit().putBoolean("seed3", true).apply();
        }
    }

    // ---------- shared helpers ----------
    static boolean hasSpace(String s) {
        for (int i = 0; i < s.length(); i++) if (Character.isWhitespace(s.charAt(i))) return true;
        return false;
    }

    static boolean usernameTaken(Context c, String u, JSONObject except) {
        JSONArray a = list(c, "accounts");
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != except && o.optString("username").equals(u)) return true;
        }
        return false;
    }

    static String randomPassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        Random r = new Random();
        StringBuilder pw = new StringBuilder();
        for (int i = 0; i < 8; i++) pw.append(chars.charAt(r.nextInt(chars.length())));
        return pw.toString();
    }

    static void bumpRoom(Context c, String hostel, String room, int d) {
        JSONArray rooms = list(c, "rooms");
        for (int i = 0; i < rooms.length(); i++) {
            JSONObject o = rooms.optJSONObject(i);
            if (o.optString("hostel").equalsIgnoreCase(hostel) && o.optString("room").equals(room))
                put(o, "occupied", String.valueOf(Math.max(0, o.optInt("occupied") + d)));
        }
        save(c, "rooms", rooms);
    }

    /** Saves a student profile, creates the login (auto username + random password) and fills the room. Returns {username, password}. */
    static String[] createStudent(Context c, JSONObject s) {
        String en = s.optString("enrollment");
        String base = s.optString("name").trim().split("\\s+")[0].toLowerCase().replaceAll("[^a-z0-9]", "");
        String tail = en.length() > 3 ? en.substring(en.length() - 3) : en;
        String user = base + tail;
        for (int n = 2; usernameTaken(c, user, null); n++) user = base + tail + n;
        String pw = randomPassword();
        add(c, "accounts", row("accounts", user, pw, "STUDENT", en));
        add(c, "students", s);
        if (!s.optString("room").isEmpty()) bumpRoom(c, s.optString("hostel"), s.optString("room"), 1);
        return new String[]{user, pw};
    }

    /** Removes the student, their login, photo, outpass/complaint/attendance records, and frees the bed. */
    static void deleteStudent(Context c, String en) {
        JSONArray st = list(c, "students"), keep = new JSONArray();
        for (int i = 0; i < st.length(); i++) {
            JSONObject s = st.optJSONObject(i);
            if (s.optString("enrollment").equals(en)) {
                if (!s.optString("room").isEmpty()) bumpRoom(c, s.optString("hostel"), s.optString("room"), -1);
            } else keep.put(s);
        }
        save(c, "students", keep);
        JSONArray acc = list(c, "accounts"), ak = new JSONArray();
        for (int i = 0; i < acc.length(); i++) {
            JSONObject o = acc.optJSONObject(i);
            if (!(o.optString("role").equals("STUDENT") && o.optString("enrollment").equals(en))) ak.put(o);
        }
        save(c, "accounts", ak);
        for (String col : new String[]{"outpasses", "complaints", "attendance"}) {
            JSONArray a = list(c, col), k = new JSONArray();
            for (int i = 0; i < a.length(); i++) if (!a.optJSONObject(i).optString("enrollment").equals(en)) k.put(a.optJSONObject(i));
            save(c, col, k);
        }
        new java.io.File(c.getFilesDir(), "photo_" + en + ".jpg").delete();
    }

    static JSONObject hostel(Context c, String name) {
        JSONArray a = list(c, "hostels");
        for (int i = 0; i < a.length(); i++) if (a.optJSONObject(i).optString("name").equals(name)) return a.optJSONObject(i);
        return null;
    }

    /** Age in whole years from a yyyy-MM-dd date, or "-" when the date can't be read. */
    static String ageFrom(String dob) {
        try {
            Calendar b = Calendar.getInstance(), n = Calendar.getInstance();
            b.setTime(new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dob));
            int age = n.get(Calendar.YEAR) - b.get(Calendar.YEAR);
            if (n.get(Calendar.DAY_OF_YEAR) < b.get(Calendar.DAY_OF_YEAR)) age--;
            return String.valueOf(Math.max(0, age));
        } catch (Exception e) { return "-"; }
    }
}
