package com.example.bluetoothcomm;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class HeartDataListAdapter extends ArrayAdapter {

    private Activity context;
    private HashMap<String, HeartData> heartDataList;
    private List<String> heartDataId;

    HeartDataListAdapter(Activity context, HashMap<String, HeartData> heartDataList, List<String> heartDataId) {
        super(context, R.layout.heart_list_item, heartDataId);
        this.context = context;
        this.heartDataId = heartDataId;
        this.heartDataList = heartDataList;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        if(convertView == null) convertView = inflater.inflate(R.layout.heart_list_item, parent, false);

        TextView rate = convertView.findViewById(R.id.rate);
        TextView date = convertView.findViewById(R.id.date);

        String dateFormat = "dd/MMM - HH:mm";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(dateFormat);

        HeartData currentData = heartDataList.get(heartDataId.get(position));
        rate.setText(String.valueOf(currentData.getHeartrate()));
        date.setText(simpleDateFormat.format(new Date(currentData.getTime())));

        return convertView;
    }
}
