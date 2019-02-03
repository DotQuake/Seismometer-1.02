package com.example.admindeveloper.seismometer.RealTimeServices;

public class RealTimeController {
    private final float alpha = 0.8f;
    private float[] gravity = {0,0,0};
    private float[] linear_acceleration = {0,0,0};
    private int x,y,z;

    public void updateXYZ(int x , int y ,int z){
        /*gravity[0] = alpha * gravity[0] + (1 - alpha) * x;
        linear_acceleration[0] = x - gravity[0];
        this.x = linear_acceleration[0];

        gravity[1] = alpha * gravity[1] + (1 - alpha) * y;
        linear_acceleration[1] = y - gravity[1];
        this.y = linear_acceleration[1];

        gravity[2] = alpha * gravity[2] + (1 - alpha) * z;
        linear_acceleration[2] = z - gravity[2];
        this.z = linear_acceleration[2];*/
        this.x=x;
        this.y=y;
        this.z=z;
    }
    public int getX() {
        return x;
    }
    public int getY() {
        return y;
    }
    public int getZ() {
        return z;
    }
}
