package com.digiihostel.app;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.widget.*;
import org.json.*;
import java.io.*;

/**
 * One form, three uses:
 *  REQUEST - from the login screen: sends a request to the admin (no login is created yet)
 *  CREATE  - admin makes a student account directly
 *  EDIT    - admin edits an existing student (and the student's login)
 */
public class RegisterActivity extends BaseActivity {
    static final int REQUEST = 0, CREATE = 1, EDIT = 2;
    Bitmap photo;
    Button photoBtn;
    StudentForm form;
    int mode;
    String oldEn;
    EditText user, pass;

    @Override void build() {
        mode = getIntent().getIntExtra("mode", REQUEST);
        oldEn = getIntent().getStringExtra("en");
        JSONObject existing = mode == EDIT ? Store.student(this, oldEn) : null;
        if (mode == EDIT && existing == null) { toast("Student not found"); finish(); return; }
        add(text(mode == REQUEST ? "New Student Registration" : mode == CREATE ? "Create Student Account" : "Edit Student", 22, true));
        if (mode == REQUEST) add(text("Your details go to the admin as a request. Your username and password are created once the admin approves it.", 12, false));

        final JSONObject acc = mode == EDIT ? accountFor(Store.list(this, "accounts"), oldEn) : null;
        if (mode == EDIT) {
            add(form_label("LOGIN - Username (no spaces)"));
            user = edit("Username"); user.setText(acc == null ? "" : acc.optString("username")); user.setSingleLine(); noSpaces(user); add(user);
            add(form_label("LOGIN - Password (no spaces)"));
            pass = edit("Password"); pass.setText(acc == null ? "" : acc.optString("password")); noSpaces(pass); eyeToggle(pass, 0xFFFFFFFF); add(pass);
        }
        form = new StudentForm(this, existing);
        form.addTo(this);

        photoBtn = button("Choose photo", v -> {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(i, 1);
        });
        add(photoBtn);
        add(button(mode == REQUEST ? "SEND REQUEST" : "SAVE", v -> save()));
    }

    TextView form_label(String s) {
        TextView t = text(s, 13, false);
        t.setTextColor(0xFF4FC3F7);
        return t;
    }

    JSONObject accountFor(JSONArray acc, String en) {
        for (int i = 0; i < acc.length(); i++) {
            JSONObject a = acc.optJSONObject(i);
            if (a.optString("role").equals("STUDENT") && a.optString("enrollment").equals(en)) return a;
        }
        return null;
    }

    boolean requestExists(String en) {
        JSONArray r = Store.list(this, "requests");
        for (int i = 0; i < r.length(); i++) if (r.optJSONObject(i).optString("enrollment").equals(en)) return true;
        return false;
    }

    void savePhoto(String fileName) {
        if (photo == null) return;
        try (FileOutputStream fo = new FileOutputStream(new File(getFilesDir(), fileName))) {
            photo.compress(Bitmap.CompressFormat.JPEG, 90, fo);
        } catch (IOException ignored) { }
    }

    void save() {
        String err = form.validate();
        if (err != null) { toast(err); return; }
        final String en = form.enrollment();
        final JSONObject s = form.toJson();
        if (mode == REQUEST) {
            if (Store.student(this, en) != null) { toast("This enrollment number is already registered"); return; }
            if (requestExists(en)) { toast("A request for this enrollment number is already waiting for the admin"); return; }
            Store.put(s, "date", Store.today());
            Store.add(this, "requests", s);
            savePhoto("req_" + en + ".jpg");
            new AlertDialog.Builder(this).setTitle("Request sent")
                    .setMessage("Your registration request has been sent to the admin. Your username and password will be created once it is approved.")
                    .setPositiveButton("OK", (d, w) -> finish()).setCancelable(false).show();
        } else if (mode == CREATE) {
            if (Store.student(this, en) != null) { toast("This enrollment number is already registered"); return; }
            if (requestExists(en)) { toast("A request with this enrollment number is pending - approve it from Requests"); return; }
            String[] cred = Store.createStudent(this, s);
            savePhoto("photo_" + en + ".jpg");
            new AlertDialog.Builder(this).setTitle("Student account created")
                    .setMessage("Username: " + cred[0] + "\nPassword: " + cred[1] + "\n\nPlease note these down.")
                    .setPositiveButton("OK", (d, w) -> finish()).setCancelable(false).show();
        } else {
            saveEdit(s, en);
        }
    }

    void saveEdit(JSONObject fresh, String newEn) {
        String u = user.getText().toString(), p = pass.getText().toString();
        if (u.isEmpty() || p.isEmpty()) { toast("Username and password can't be empty"); return; }
        if (Store.hasSpace(u) || Store.hasSpace(p)) { toast("Spaces are not allowed in username or password"); return; }
        JSONArray st = Store.list(this, "students"), acc = Store.list(this, "accounts");
        JSONObject a = accountFor(acc, oldEn);
        boolean isNew = a == null;
        if (isNew) a = Store.row("accounts", "", "", "STUDENT", oldEn);
        if (Store.usernameTaken(this, u, a)) { toast("Username already taken"); return; }
        int idx = -1;
        for (int i = 0; i < st.length(); i++) {
            String e = st.optJSONObject(i).optString("enrollment");
            if (e.equals(oldEn)) idx = i;
            else if (e.equals(newEn)) { toast("Enrollment already used by another student"); return; }
        }
        if (idx < 0) { toast("Student not found"); return; }
        JSONObject s = st.optJSONObject(idx);
        String oldHostel = s.optString("hostel"), oldRoom = s.optString("room");
        for (String k : Store.F.get("students")) Store.put(s, k, fresh.optString(k));
        Store.put(a, "username", u);
        Store.put(a, "password", p);
        Store.put(a, "enrollment", newEn);
        if (isNew) acc.put(a);
        Store.save(this, "students", st);
        Store.save(this, "accounts", acc);
        // keep outpasses / complaints / attendance linked to the student
        for (String col : new String[]{"outpasses", "complaints", "attendance"}) {
            JSONArray arr = Store.list(this, col);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o.optString("enrollment").equals(oldEn)) {
                    Store.put(o, "enrollment", newEn);
                    if (o.has("name")) Store.put(o, "name", s.optString("name"));
                }
            }
            Store.save(this, col, arr);
        }
        if (!newEn.equals(oldEn)) new File(getFilesDir(), "photo_" + oldEn + ".jpg").renameTo(new File(getFilesDir(), "photo_" + newEn + ".jpg"));
        savePhoto("photo_" + newEn + ".jpg");
        if (!oldHostel.equalsIgnoreCase(s.optString("hostel")) || !oldRoom.equals(s.optString("room"))) {
            if (!oldRoom.isEmpty()) Store.bumpRoom(this, oldHostel, oldRoom, -1);
            if (!s.optString("room").isEmpty()) Store.bumpRoom(this, s.optString("hostel"), s.optString("room"), 1);
        }
        toast("Saved");
        finish();
    }

    @Override protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == 1 && res == RESULT_OK && data != null) {
            try {
                Uri u = data.getData();
                Bitmap b = BitmapFactory.decodeStream(getContentResolver().openInputStream(u));
                float s = Math.min(1f, 400f / Math.max(b.getWidth(), b.getHeight()));
                photo = Bitmap.createScaledBitmap(b, (int) (b.getWidth() * s), (int) (b.getHeight() * s), true);
                photoBtn.setText("Photo selected");
            } catch (Exception e) { toast("Could not load photo"); }
        }
    }
}
