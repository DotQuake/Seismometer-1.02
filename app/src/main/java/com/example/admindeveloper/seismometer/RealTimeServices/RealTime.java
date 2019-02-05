package com.example.admindeveloper.seismometer.RealTimeServices;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.admindeveloper.seismometer.DisplayGraph;
import com.example.admindeveloper.seismometer.R;

import android.widget.TextView;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.RecordSaveData;
import com.jjoe64.graphview.GraphView;

import java.util.Calendar;
import java.util.Date;

public class RealTime extends Fragment{

    View myView;
    RealTimeController rtc;
    RecordSaveData rsdata;
    BroadcastReceiver br;
    DisplayGraph dataGraphController;
    GraphView dataGraph;
    TextView hourBox,minuteBox;

    private void displayData(Short x , Short y , Short z) {
        rtc.updateXYZ(x,y,z);
        dataGraphController.displayRawDataGraph(rtc.getX(),rtc.getY(),rtc.getZ());
        Date currentDate=Calendar.getInstance().getTime();
        hourBox.setText(currentDate.getHours()+" ");
        minuteBox.setText(currentDate.getMinutes()+" ");
    }

    public void showMessage(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this.getActivity());
        builder.setCancelable(false);
        builder.setTitle("Real Time");
        builder.setMessage("Please put your device on a Flat Surface Area");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Toast.makeText(getContext(),"Thank you", Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    //-----------------------------------------------------------------------------------------------------

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.realtimelayout,container,false);
        rtc = new RealTimeController();
        rsdata = new RecordSaveData();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
        }
        //showMessage();
        dataGraph=myView.findViewById(R.id.dataGraph);
        dataGraphController=new DisplayGraph(dataGraph,500);
        hourBox=myView.findViewById(R.id.hourBox);
        minuteBox=myView.findViewById(R.id.minuteBox);
        return myView;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 1000:{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(getActivity(),"Permission Granted", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(getActivity(),"Permission not granted", Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            }
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void onResume() {
        super.onResume();


        if(br == null) {
            br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    displayData(intent.getShortExtra("valueX",(short)0)
                            ,intent.getShortExtra("valueY",(short)0)
                            ,intent.getShortExtra("valueZ",(short)0));
                }
            };
        }
        IntentFilter filt = new IntentFilter("FILTER"); // before
        getActivity().registerReceiver(br, filt);// before
        /* AFTER
         registerReceiver(br,new IntentFilter("location_update"));
         */
    }

    @Override
    public void onPause() {
        super.onPause();
        if(br != null) {
            getActivity().unregisterReceiver(br); // put unregister here in on pause so that it will unregister if
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
    //-----------------------------------------------------------------------------------------------------


}