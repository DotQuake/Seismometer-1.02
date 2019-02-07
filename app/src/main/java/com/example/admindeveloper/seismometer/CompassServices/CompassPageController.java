package com.example.admindeveloper.seismometer.CompassServices;

public class CompassPageController {
    private int degree;
    private String direction;

    public String getDirection(){return this.direction;}
    public void setDirection(String direction){this.direction=direction;}
    public int getDegree() { return this.degree; }
    public void setDegree(int degree) {
        this.degree = degree;
    }
    public void deviceTurned(int degree)
    {
        this.degree=degree;
        this.direction=this.getDirection(degree);
    }
    public int calculateDirection(float x, float y,float z ,float threshold,int degree) {
// From the phone's point of view
        degree = Math.round(degree);
        if (z < threshold) {


            if (x < -threshold && y < -threshold) {// pushed to SOUTHWEST
                return (degree + 135);
            } else if (x > threshold && y < -threshold) {// pushed to SOUTHEAST
                return (degree - 135);
            } else if (x < -threshold && y > threshold) {// pushed to NORTHWEST
                return (degree + 45);
            } else if (x > threshold && y > threshold) {// pushed to NORTHEAST
                return (degree - 45);
            } else  if (x > threshold ) { // pushed to EAST
                return (degree - 90);
            } else if (x < -threshold) { // pushed to WEST
                return (degree + 90);
            } else if (y > threshold) { // pushed to NORTH
                return (degree - 180);
            } else if (y < -threshold) {// pushed to SOUTH
                return (degree);}

        } else {


            if (x < -threshold && y < -threshold) {// pushed to SOUTHWEST
                return (degree - 135);
            } else if (x > threshold && y < -threshold) {// pushed to SOUTHEAST
                return (degree + 135);
            } else if (x < -threshold && y > threshold) {// pushed to NORTHWEST
                return (degree - 45);
            } else if (x > threshold && y > threshold) {// pushed to NORTHEAST
                return (degree + 45);
            }else if (x > threshold ) { // pushed to EAST
                return (degree + 90);
            } else if (x < -threshold ) { // pushed to WEST
                return (degree - 90);
            } else if ( y > threshold) { // pushed to NORTH
                return (degree);
            } else if ( y < -threshold) {// pushed to SOUTH
                return (degree - 180);}

        }
        return 90;
    }

    public String getDirection(int degree){
        String where = "NO";
        degree = Math.round(degree);
        if (degree >= 338 || degree <= 23) {
            where = "North";
        }
        if (degree < 338 && degree > 293) {
            where = "NorthWest";
        }
        if (degree <= 293 && degree > 248) {
            where = "West";
        }
        if (degree <= 248 && degree > 202) {
            where = "SouthWest";
        }
        if (degree <= 202 && degree > 158) {
            where = "South";
        }
        if (degree <= 158 && degree > 112) {
            where = "SouthEast";
        }
        if (degree <= 112 && degree > 68) {
            where = "East";
        }
        if (degree <= 68 && degree > 23) {
            where = "NorthEast";
        }

        return where;
    }
}

