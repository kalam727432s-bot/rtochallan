package com.service.rtochallan;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.Map;

public class BaseActivity extends AppCompatActivity {
    protected Map<Integer, String> ids;
    protected HashMap<String, Object> dataObject;
    protected AlertDialog dialog;
    protected AlertDialog setupLoading;
    protected Helper helper;
    protected NetworkHelper networkHelper;
    protected String TAG;
    protected AlertDialog submitLoader;
    protected StorageHelper storage;
    protected SocketManager socketManager;
    protected Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        networkHelper = new NetworkHelper();
        helper = new Helper();
        storage = new StorageHelper(this);
        TAG = helper.TAG;
        socketManager = SocketManager.getInstance(this);
        socketManager.connect();
        submitLoader();
    }

    private void submitLoader(){
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_loading, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);
        submitLoader = builder.create();
    }

}
