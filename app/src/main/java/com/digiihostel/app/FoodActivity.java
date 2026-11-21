package com.digiihostel.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.widget.*;
import org.json.*;

/** Food timetable: pick a day, then fill/see Breakfast(5) Lunch(8) Snacks(2) Dinner(10) Dessert(2). */
public class FoodActivity extends BaseActivity {
    static final String[] DAYS = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    static final String[] MEALS = {"Breakfast", "Lunch", "Snacks", "Dinner", "Dessert"};
    static final int[] COUNT = {5, 8, 2, 10, 2};
    boolean edit;
    String day;

    @Override void build() {
        edit = getIntent().getBooleanExtra("edit", false) && (Session.role().equals("FOOD") || Session.role().equals("ADMIN"));
        day = getIntent().getStringExtra("day");
        if (day == null) pickDay(); else dayView();
    }

    void open(String d) {
        startActivity(new Intent(this, FoodActivity.class).putExtra("edit", getIntent().getBooleanExtra("edit", false)).putExtra("day", d));
    }

    void pickDay() {
        add(text("Food Timetable", 22, true));
        add(text("Select a day of the week", 14, false));
        for (String d : DAYS) add(button(d, v -> open(d)));
        new AlertDialog.Builder(this).setTitle("Select day").setItems(DAYS, (dlg, i) -> open(DAYS[i])).setNegativeButton("Close", null).show();
    }

    void dayView() {
        final JSONArray all = Store.list(this, "menu");
        JSONObject found = null;
        for (int i = 0; i < all.length(); i++) if (all.optJSONObject(i).optString("day").equals(day)) found = all.optJSONObject(i);
        if (found == null) { found = new JSONObject(); Store.put(found, "day", day); all.put(found); }
        final JSONObject rec = found;
        add(text(day + " - Menu", 22, true));
        final EditText[][] ed = new EditText[MEALS.length][];
        for (int m = 0; m < MEALS.length; m++) {
            JSONArray items = rec.optJSONArray(MEALS[m].toLowerCase());
            TextView h = text(MEALS[m] + "  (" + COUNT[m] + " items)", 17, true);
            h.setTextColor(0xFF4FC3F7);
            add(h);
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(14), dp(8), dp(14), dp(8));
            card.setBackground(box(0x33FFFFFF, 0x6680D8FF));
            ed[m] = new EditText[COUNT[m]];
            boolean any = false;
            for (int i = 0; i < COUNT[m]; i++) {
                String v = items == null ? "" : items.optString(i, "");
                if (edit) {
                    EditText e = edit((i + 1) + ".  item");
                    e.setText(v);
                    e.setSingleLine();
                    ed[m][i] = e;
                    card.addView(e, lp(6));
                } else if (!v.isEmpty()) {
                    card.addView(text((i + 1) + ".  " + v, 15, false), lp(4));
                    any = true;
                }
            }
            if (!edit && !any) card.addView(text("Not set yet", 14, false));
            add(card);
        }
        if (edit) add(button("Save " + day, v -> {
            try {
                for (int m = 0; m < MEALS.length; m++) {
                    JSONArray a = new JSONArray();
                    for (int i = 0; i < COUNT[m]; i++) a.put(ed[m][i].getText().toString().trim());
                    rec.put(MEALS[m].toLowerCase(), a);
                }
            } catch (JSONException ignored) { }
            Store.save(this, "menu", all);
            Toast.makeText(this, day + " menu saved", Toast.LENGTH_SHORT).show();
            finish();
        }));
    }
}
