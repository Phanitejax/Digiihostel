package com.digiihostel.app;

import org.json.JSONObject;

class Session {
    static JSONObject me;
    static String role() { return me == null ? "" : me.optString("role"); }
    static String enroll() { return me == null ? "-" : me.optString("enrollment"); }
}
