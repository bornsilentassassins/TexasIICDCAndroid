package com.example.bluetoothcomm;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    DatabaseReference databaseReference;
    HashMap<String, HeartData> heartDataList;
    List<String> heartDataId;

    int max = 0;
    int min = 10000;
    int sum = 0;

    TextView maxView;
    TextView minView;
    TextView avgView;
    ListView heartDataListView;
    HeartDataListAdapter adapter;

    TextView status;
    Button toggle;

    String deviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        heartDataList = new HashMap<>();
        heartDataId = new ArrayList<>();

        maxView = findViewById(R.id.max);
        minView = findViewById(R.id.min);
        avgView = findViewById(R.id.avg);
        heartDataListView = findViewById(R.id.heartdata);
        status = findViewById(R.id.connect_status);
        toggle = findViewById(R.id.toggle_connect);

        adapter = new HeartDataListAdapter(this, heartDataList, heartDataId);
        heartDataListView.setAdapter(adapter);

        toggleStatus();

        databaseReference = FirebaseDatabase.getInstance().getReference().child("heart");
        long ct = System.currentTimeMillis() / 86400000L;
        databaseReference = databaseReference.child(String.valueOf(ct));
        databaseReference.addChildEventListener(eventListener);

        toggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent foreground = new Intent(MainActivity.this, BluetoothCommService.class);
                if(deviceName == null) foreground.setAction("connect");
                else foreground.setAction("diconnect");
                startService(foreground);
                // Todo: Run on seperate thread with inteval of 1 sec
                final Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (BluetoothCommService.getStatus() == 0) {
                                this.wait(1000);
                                Log.e("MainActivity", "Refreshing");
                            }
                        } catch (InterruptedException e) {
                            finish();
                        }
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggleStatus();
                            }
                        });
                    }
                });
                thread.start();

            }
        });
    }

    void toggleStatus() {
        try {
            deviceName = BluetoothCommService.getConnectedDevice();
        } catch (Exception e) {
            deviceName = null;
        }

        if (deviceName == null) {
            toggle.setText("Refresh");
            status.setText("No device connected");
        } else {
            toggle.setText("Disconnect");
            status.setText(deviceName + " connected");
        }
    }

    ChildEventListener eventListener = new ChildEventListener() {
        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            HeartData curentData = dataSnapshot.getValue(HeartData.class);
            int heartRate = curentData.getHeartrate();
            sum += heartRate;
            if(heartRate > max) max = heartRate;
            if(heartRate < min) min = heartRate;
            heartDataList.put(dataSnapshot.getKey(), curentData);
            heartDataId.add(dataSnapshot.getKey());

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    maxView.setText(String.valueOf(max));
                    minView.setText(String.valueOf(min));
                    avgView.setText(String.format("%.2f", (float) sum / heartDataList.size() ));
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            HeartData curentData = dataSnapshot.getValue(HeartData.class);
            HeartData oldData = heartDataList.get(dataSnapshot.getKey());
            int heartRate = curentData.getHeartrate();
            sum += heartRate - oldData.getHeartrate();
            if(heartRate > max) max = heartRate;
            if(heartRate < min) min = heartRate;
            heartDataList.put(dataSnapshot.getKey(), curentData);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    maxView.setText(String.valueOf(max));
                    minView.setText(String.valueOf(min));
                    avgView.setText(String.format("%.2f", (float) sum / heartDataList.size() ));
                    adapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {

        }
    };


}
