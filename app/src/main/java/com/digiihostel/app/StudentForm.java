package com.digiihostel.app;

import android.graphics.Rect;
import android.text.InputType;
import android.view.View;
import android.widget.*;
import org.json.*;
import java.util.*;

/** The student details form shared by registration requests, admin "new student" and admin edit. All the input rules live here. */
class StudentForm {
    static final String[] GENDERS = {"Male", "Female", "Other"};
    static final String[] BLOODS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    static final String[] YEARS = {"1st Year", "2nd Year", "3rd Year", "4th Year"};

    final BaseActivity a;
    final JSONObject pre;
    final List<View> views = new ArrayList<>();
    EditText name, enr, dob, phone, email, parent, room;
    Spinner gender, year, school, branch, hostel, blood;
    LinearLayout branchBox;
    TextView hostelNote;
    int lastSchool, lastGender;

    StudentForm(BaseActivity act, JSONObject prefill) {
        a = act;
        pre = prefill == null ? new JSONObject() : prefill;

        name = a.edit("Name");
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        name.setText(pre.optString("name"));

        enr = a.edit("Enrollment No (12 digits)");
        enr.setText(pre.optString("enrollment"));      // text first, so an old-style enrollment is shown (and flagged) instead of being cut
        a.digitsOnly(enr, 12);

        gender = a.spinner(GENDERS);
        gender.setSelection(Math.max(0, Arrays.asList(GENDERS).indexOf(pre.optString("gender"))));
        lastGender = gender.getSelectedItemPosition();

        dob = a.dateField("Date of birth (tap to slide & choose)", false, 2005);
        dob.setText(pre.optString("dob"));

        // school -> branch
        List<String> schools = new ArrayList<>();
        schools.add("Select school");
        schools.addAll(Arrays.asList(Catalog.SCHOOLS));
        String ps = pre.optString("school");
        if (!ps.isEmpty() && !schools.contains(ps)) schools.add(1, ps);     // keep an older, free-typed value visible
        school = a.spinnerOf(schools, true);
        lastSchool = Math.max(0, schools.indexOf(ps));
        school.setSelection(lastSchool);
        branch = a.spinnerOf(new ArrayList<>(Arrays.asList("Select branch")), true);
        branchBox = new LinearLayout(a);
        branchBox.setOrientation(LinearLayout.VERTICAL);
        branchBox.addView(label("Branch / course"));
        branchBox.addView(branch, a.lp(4));
        branchBox.setVisibility(View.GONE);
        if (lastSchool > 0) fillBranches(lastSchool, pre.optString("branch"), false);
        school.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos == lastSchool) return;
                lastSchool = pos;
                fillBranches(pos, null, true);
                if (pos > 0) branchBox.post(() -> branch.performClick());      // open the course list straight away
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        year = a.spinner(YEARS);
        year.setSelection(Math.max(0, Math.min(3, pre.optInt("year", 1) - 1)));

        phone = a.edit("Phone number (10 digits)");
        phone.setText(pre.optString("phone"));
        a.digitsOnly(phone, 10);

        email = a.edit("E-mail");
        email.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        email.setText(pre.optString("email"));

        parent = a.edit("Parent contact (10 digits)");
        parent.setText(pre.optString("parent"));
        a.digitsOnly(parent, 10);

        hostel = a.spinnerOf(new ArrayList<>(Arrays.asList("Select hostel")), true);
        hostelNote = a.text("", 11, false);
        refreshHostels(pre.optString("hostel"));
        gender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (pos == lastGender) return;
                lastGender = pos;
                refreshHostels(sel(hostel));
            }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        room = a.edit("Room No");
        room.setText(pre.optString("room"));
        blood = a.spinner(BLOODS);
        blood.setSelection(Math.max(0, Arrays.asList(BLOODS).indexOf(pre.optString("blood"))));

        views.add(name); views.add(enr);
        views.add(label("Gender")); views.add(gender);
        views.add(dob);
        views.add(label("School")); views.add(school);
        views.add(branchBox);
        views.add(label("Year")); views.add(year);
        views.add(phone); views.add(email); views.add(parent);
        views.add(label("Hostel")); views.add(hostel); views.add(hostelNote);
        views.add(room);
        views.add(label("Blood group")); views.add(blood);
    }

    TextView label(String s) {
        TextView t = a.text(s, 13, false);
        t.setTextColor(0xFF4FC3F7);
        return t;
    }

    void addTo(BaseActivity act) { for (View v : views) act.add(v); }

    static String sel(Spinner s) { Object o = s.getSelectedItem(); return o == null ? "" : o.toString(); }

    void fillBranches(int schoolPos, String keep, boolean animate) {
        String sc = (String) school.getItemAtPosition(schoolPos);
        List<String> items = new ArrayList<>();
        items.add("Select branch");
        items.addAll(Arrays.asList(Catalog.branchesOf(sc)));
        if (keep != null && !keep.isEmpty() && !items.contains(keep)) items.add(1, keep);
        branch.setAdapter(new BaseActivity.HintAdapter(a, items, true));
        branch.setSelection(Math.max(0, keep == null ? 0 : items.indexOf(keep)));
        if (branchBox.getVisibility() != View.VISIBLE) {
            branchBox.setVisibility(View.VISIBLE);
            if (animate) {
                branchBox.setAlpha(0f); branchBox.setPivotY(0f); branchBox.setScaleY(0.5f);
                branchBox.animate().alpha(1f).scaleY(1f).setDuration(350).start();
            }
        }
    }

    /** Hostels the chosen gender may book: Male -> Boys, Female -> Girls, Other -> any. A hostel with no gender set yet is open to all. */
    void refreshHostels(String keep) {
        String g = sel(gender);
        JSONArray hs = Store.list(a, "hostels");
        List<String> items = new ArrayList<>();
        items.add("Select hostel");
        for (int i = 0; i < hs.length(); i++) {
            JSONObject h = hs.optJSONObject(i);
            String hg = h.optString("gender");
            if (hg.isEmpty() || g.equals("Other") || (g.equals("Male") && hg.equals("Boys")) || (g.equals("Female") && hg.equals("Girls")))
                items.add(h.optString("name"));
        }
        hostel.setAdapter(new BaseActivity.HintAdapter(a, items, true));
        hostel.setSelection(Math.max(0, keep == null ? 0 : items.indexOf(keep)));
        hostelNote.setText(g.equals("Male") ? "Showing boys' hostels only" : g.equals("Female") ? "Showing girls' hostels only" : "Showing all hostels");
    }

    String fail(View v, String msg) {
        if (v instanceof EditText) ((EditText) v).setError(msg);
        v.requestRectangleOnScreen(new Rect(0, 0, v.getWidth(), v.getHeight()));
        return msg;
    }

    /** Returns null when everything is valid, otherwise the first problem (also marked on the field). */
    String validate() {
        if (name.getText().toString().trim().isEmpty()) return fail(name, "Enter the student's name");
        if (enr.getText().toString().length() != 12) return fail(enr, "Enrollment number must be exactly 12 digits");
        if (dob.getText().toString().isEmpty()) return fail(dob, "Choose the date of birth");
        if (school.getSelectedItemPosition() < 1) return fail(school, "Select a school");
        if (branch.getSelectedItemPosition() < 1) return fail(branch, "Select a branch");
        if (phone.getText().toString().length() != 10) return fail(phone, "Phone number must be exactly 10 digits");
        if (!a.looksLikeEmail(email.getText().toString().trim())) return fail(email, "Enter a valid e-mail address");
        if (parent.getText().toString().length() != 10) return fail(parent, "Parent contact must be exactly 10 digits");
        if (hostel.getSelectedItemPosition() < 1) return fail(hostel, "Select a hostel for this gender");
        return null;
    }

    String enrollment() { return enr.getText().toString().trim(); }

    JSONObject toJson() {
        JSONObject o = new JSONObject();
        Store.put(o, "enrollment", enrollment());
        Store.put(o, "name", name.getText().toString().trim());
        Store.put(o, "gender", sel(gender));
        Store.put(o, "dob", dob.getText().toString().trim());
        Store.put(o, "branch", sel(branch));
        Store.put(o, "year", String.valueOf(year.getSelectedItemPosition() + 1));
        Store.put(o, "school", sel(school));
        Store.put(o, "phone", phone.getText().toString().trim());
        Store.put(o, "email", email.getText().toString().trim());
        Store.put(o, "parent", parent.getText().toString().trim());
        Store.put(o, "hostel", sel(hostel));
        Store.put(o, "room", room.getText().toString().trim());
        Store.put(o, "blood", sel(blood));
        return o;
    }
}
