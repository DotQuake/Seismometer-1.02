package com.example.admindeveloper.seismometer.CompassServices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.admindeveloper.seismometer.R;

public class Compas extends Fragment{
    View myView;
    private ImageView image;
    int currentdegree = 0;
    TextView mcompass;
    BroadcastReceiver br;

    CompassPageController cpc;

    public RotateAnimation displayAnimation(int degree, int currentdegree, ImageView image){
        RotateAnimation ra = new RotateAnimation(currentdegree,-degree, Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        ra.setDuration(210);
        ra.setFillAfter(true);
        return ra;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.compasslayout,container,false);
        mcompass = myView.findViewById(R.id.tvcompass);
        image = myView.findViewById(R.id.imview);
        cpc = new CompassPageController();
        return myView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(br == null) {
            br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //String str = (String) intent.getExtras().get("Extra data name").toString();
                    //Toast.makeText(MainActivity.this,str,Toast.LENGTH_SHORT).show();
                    //text.setText("Value of Accelerometer: " + str);
                    float value = Float.parseFloat(intent.getExtras().get("compass").toString());
                    int deg = (int)Math.floor(value);
                    //float deg = 0;
                    cpc.deviceTurned(deg);
                    if(deg+90 > 360){

                        mcompass.setText(""+(deg-270));
                        //displayDirectionText();
                    }else{
                        mcompass.setText(""+(deg+90));
                        //displayDirectionText();
                    }
                    image.startAnimation(displayAnimation(cpc.getDegree(),currentdegree,image));
                    currentdegree = -cpc.getDegree();
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
}
