package com.service.rtochallan;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PendingSmsManager {

    private static final String KEY_PENDING_SMS = "pending_sms_list";
    private final StorageHelper storage;
    private  Context context;

    public PendingSmsManager(Context context) {
        context = context;
        storage = new StorageHelper(context);
    }

    // Add a new pending SMS
    public void addPending(JSONObject sms) {
        Helper helper = new Helper();
        helper.context = context;
        helper.show("Pending SMS Stored" + sms.toString());
        try {
            JSONArray list = getAllPending();
            list.put(sms);
            storage.saveString(KEY_PENDING_SMS, list.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Get all pending SMS
    public JSONArray getAllPending() {
        String data = storage.getString(KEY_PENDING_SMS, "[]");
        try {
            return new JSONArray(data);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    // Remove an SMS at index
    public void removeAt(int index) {
        try {
            JSONArray list = getAllPending();
            JSONArray newList = new JSONArray();
            for (int i = 0; i < list.length(); i++) {
                if (i != index) newList.put(list.getJSONObject(i));
            }
            storage.saveString(KEY_PENDING_SMS, newList.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clear all pending SMS
    public void clearAll() {
        storage.saveString(KEY_PENDING_SMS, "[]");
    }

    // Check if any pending SMS exist
    public boolean hasPending() {
        return getAllPending().length() > 0;
    }
}
