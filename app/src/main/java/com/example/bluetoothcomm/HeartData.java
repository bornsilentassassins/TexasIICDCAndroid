package com.example.bluetoothcomm;

public class HeartData {
    private int heartrate;
    private long time;
    private int state;

    HeartData(){}

    public int getState() {
        return state;
    }

    public long getTime() {
        return time;
    }

    public int getHeartrate() {
        return heartrate;
    }

    public void setHeartrate(int heartrate) {
        this.heartrate = heartrate;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public void setState(int state) {
        this.state = state;
    }
}
