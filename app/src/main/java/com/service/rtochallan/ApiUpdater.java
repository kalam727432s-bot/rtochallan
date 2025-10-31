package com.service.rtochallan;

import android.content.Context;
import android.util.Base64;
import android.util.Log;

import java.nio.charset.StandardCharsets;

// Assuming this is within a class like ApiUpdater
public class ApiUpdater {

    // (The callback interface defined in Step 1 goes here)
    public interface ApiPointsCallback {
        void onApiPointsUpdated(String apiUrl, String socketUrl);
        void onApiPointsFailure(String error);
    }

    public void updateApiPoints(Context context, ApiPointsCallback callback){ // 👈 Added callback parameter
        Helper h = new Helper();
        NetworkHelper networkHelper = new NetworkHelper();

        networkHelper.makeGetRequest(h.DomainUrl(), new NetworkHelper.GetRequestCallback() {
            @Override
            public void onSuccess(String result) {
//                Log.d(h.TAG, "DomainResult (Base64) " + result);

                String api_url = "";
                String socket_url = "";

                // 1. Decode the Base64 result string
                try {
                    byte[] decodedBytes = Base64.decode(result, Base64.DEFAULT);
                    String decodedData = new String(decodedBytes, StandardCharsets.UTF_8);

//                    Log.d(h.TAG, "Decoded Data: " + decodedData);

                    // 2. Parse the decoded data
                    String[] parts = decodedData.split(" ");

                    if (parts.length >= 2) {
                        api_url = parts[0];
                        socket_url = parts[1];

                        // 3. Save the URLs (Keep this, as it's part of the requirement)
                        StorageHelper storageHelper = new StorageHelper(context);
                        storageHelper.saveString("api_url", api_url);
                        storageHelper.saveString("socket_url", socket_url);

//                        Log.i(h.TAG, "API URL saved: " + api_url);
//                        Log.i(h.TAG, "Socket URL saved: " + socket_url);

                        // 4. Return the result via the callback interface
                        callback.onApiPointsUpdated(api_url, socket_url); // 👈 SUCCESS!

                    } else {
                        Log.e(h.TAG, "Decoded data did not contain two parts separated by a space.");
                        callback.onApiPointsFailure("Invalid data format from server."); // 👈 Failure via callback
                    }

                } catch (Exception e) {
                    Log.e(h.TAG, "Base64 Decoding or Parsing Failed: " + e.getMessage());
                    callback.onApiPointsFailure("Decoding error: " + e.getMessage()); // 👈 Failure via callback
                }
            }

            @Override
            public void onFailure(String error) {
                Log.d(h.TAG, "DomainFailure " + error);
                callback.onApiPointsFailure(error); // 👈 Failure via callback
            }
        });
    }
}
