package com.example.bluetoothcomm;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

public class BluetoothCommService extends Service {

    BluetoothAdapter bluetoothAdapter;
    BluetoothSocket btSocket;
    BluetoothDevice btDevice;
    OutputStream outputStream;
    InputStream inputStream;
    Thread workerThread;
    Boolean stopWorker;
    int readBufferPosition;
    byte[] readBuffer;

    private static String CONNECTED_DEVICE = null;
    private static int STATUS = 1;

    DatabaseReference dbReference;

    @Override
    public void onCreate() {
        super.onCreate();
        STATUS = 0;
        dbReference = FirebaseDatabase.getInstance().getReference().child("heart");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if("connect".equals(intent.getAction())) {
            Intent startConnect = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, startConnect, 0);

            Notification.Builder notificationBuilder;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
                String channelName = "My Background Service";
                NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
                chan.setLightColor(Color.BLUE);
                chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
                NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                assert manager != null;
                manager.createNotificationChannel(chan);
                notificationBuilder = new Notification.Builder(this, NOTIFICATION_CHANNEL_ID);
            } else {
                notificationBuilder = new Notification.Builder(this);
            }

            Notification notification = notificationBuilder.setContentTitle("Test")
                    .setContentText("test text")
                    .setContentIntent(pendingIntent)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setTicker("Description")
                    .setOngoing(true)
                    .build();

            startForeground(1, notification);

            try {
                findBT();
                if (btSocket == null) openBT();
            } catch (IOException e) {
                stopForeground(true);
                onDestroy();
            }

            STATUS = 1;
            return START_STICKY;
        } else {
            stopForeground(true);
            onDestroy();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        closeBT();
        super.onDestroy();
    }


    void findBT()
    {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null)
        {
            Log.e("Service", "no btAdapter");
        }

        if(!bluetoothAdapter.isEnabled())
        {
            Log.e("Service", "bt not enabled");
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0 && btDevice==null)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getAddress().equals(getString(R.string.MAC)))
                {
                    Log.e("Service", "bt connected");
                    CONNECTED_DEVICE = device.getName();
                    btDevice = device;
                    break;
                }
            }
        }
    }

    void openBT() throws IOException
    {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        btSocket = btDevice.createRfcommSocketToServiceRecord(uuid);
        btSocket.connect();
        outputStream = btSocket.getOutputStream();
        inputStream = btSocket.getInputStream();

        beginListenForData();
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = inputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            inputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];
                                if(b == delimiter)
                                {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, StandardCharsets.US_ASCII);
                                    readBufferPosition = 0;

                                    handler.post(new Runnable()
                                    {
                                        public void run()
                                        {
                                            HeartData heartData = new HeartData();
                                            heartData.setHeartrate(Integer.valueOf(data));
                                            long ct = System.currentTimeMillis();
                                            heartData.setTime(ct);
                                            heartData.setState(0);
                                            dbReference.child(String.valueOf(ct / 86400000L)).push().setValue(heartData);
                                        }
                                    });
                                }
                                else
                                {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

    void sendData() throws IOException
    {
        String msg = "OK\n";
        outputStream.write(msg.getBytes());
    }

    void closeBT()
    {
        try {
            STATUS = 1;
            stopWorker = true;
            if(outputStream != null) outputStream.close();
            if(inputStream != null) inputStream.close();
            btSocket.close();
        } catch (IOException e) {
            outputStream = null;
            inputStream = null;
        } finally {
            btDevice = null;
            btSocket = null;
            CONNECTED_DEVICE = null;
        }
    }

    public static String getConnectedDevice(){
        return CONNECTED_DEVICE;
    }

    public static int getStatus() {
        return STATUS;
    }
}
