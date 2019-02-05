package com.example.admindeveloper.seismometer.RealTimeServices;

import android.content.IntentFilter;

public class RealTimeController {
    private static Integer calibrateX=0,calibrateY=0,calibrateZ=0;
    private Integer x,y,z;

    public void updateXYZ(Short x , Short y ,Short z){
        this.x=Integer.parseInt(String.valueOf(x))-RealTimeController.calibrateX;
        this.y=Integer.parseInt(String.valueOf(y))-RealTimeController.calibrateY;
        this.z=Integer.parseInt(String.valueOf(z))-RealTimeController.calibrateZ;
    }

    public Integer getX() {
        return x;
    }
    public Integer getY() {
        return y;
    }
    public Integer getZ() {
        return z;
    }
    public void setCalibrationValue(Integer x,Integer y,Integer z){
        RealTimeController.calibrateX=x;
        RealTimeController.calibrateY=y;
        RealTimeController.calibrateZ=z;
    }
}
