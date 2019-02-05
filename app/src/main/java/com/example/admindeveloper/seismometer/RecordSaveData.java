package com.example.admindeveloper.seismometer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class RecordSaveData {
    private List<Integer> x_values = new ArrayList<>();
    private List<Integer> y_values = new ArrayList<>();
    private List<Integer> z_values = new ArrayList<>();
    private List<String> time_values = new ArrayList<>();
    private int sum_of_samples = 0;
    public void clearData()
    {
        x_values.clear();
        y_values.clear();
        z_values.clear();
        time_values.clear();
    }
    public void recordData(Integer x, Integer y, Integer z, String time){
        x_values.add(x);
        y_values.add(y);
        z_values.add(z);
        time_values.add(time);
    }

    public String saveEarthquakeData(String authority, String fileName, String gpslong, String gpslat, String compass, Boolean append, int iappendctr , int limitappend)
    {
        final int samplePerSecond=30;
        sum_of_samples += x_values.size();
        String[] separated = fileName.split("-");
        String year = separated[0];
        String month = separated[1];
        String day = separated[2];
        String hour = separated[3];
        String minute = separated[4];
        String second = separated[5];
        File myDir = new File("storage/emulated/0/Samples");
        if(!myDir.exists())
        {
            myDir.mkdirs();
        }
        File file = new File(myDir,fileName);
        try {
            file.createNewFile();
            FileOutputStream fos =  new FileOutputStream(file,append);
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
            if(!append) {
                bw.write("ARRIVALS,,,,\r\n");
                bw.write("#sitename,,,,\r\n");
                bw.write("#onset,,,,\r\n");
                bw.write("#first motion,,,,\r\n");
                bw.write("#phase,,,,\r\n");
                bw.write("#year month day,,,,\r\n");
                bw.write("#hour minute,,,,\r\n");
                bw.write("#second,,,,\r\n");
                bw.write("#uncertainty in seconds,,,,\r\n");
                bw.write("#peak amplitude,,,,\r\n");
                bw.write("#frequency at P phase,,,,\r\n");
                bw.write(",,,,\r\n");
                bw.write("longitude : " + gpslong + "\r\n");
                bw.write("latitude : " + gpslat + "\r\n");
                bw.write("compass : " + compass + "\r\n");
                bw.write("TIME SERIES,,,,\r\n");
                bw.write("LLPS,LLPS,LLPS,#sitename,\r\n");
                bw.write("EHE _,EHN _,EHZ _,#component,\r\n");
                bw.write(authority + "," + authority + "," + authority + ",#authority,\r\n");
                String hold = year;
                hold = Integer.parseInt(month) <= 9 ? hold + "0" + Integer.parseInt(month) : hold + Integer.parseInt(month);
                hold = Integer.parseInt(day) <= 9 ? hold + "0" + Integer.parseInt(day) : "" + hold + Integer.parseInt(day);
                bw.write(hold + "," + hold + "," + hold + ",#year month day,\r\n");
                hold = hour;
                hold = Integer.parseInt(minute) <= 9 ? hold + "0" + Integer.parseInt(minute) : "" + hold + Integer.parseInt(minute);
                bw.write(hold + "," + hold + "," + hold + ",#hour minute,\r\n");
                bw.write(second + "," + second + "," + second + ",#second,\r\n");
                bw.write(samplePerSecond + "," + samplePerSecond + "," + samplePerSecond + ",#samples per second,\r\n");
              //  bw.write(sum_of_samples + "," + sum_of_samples + "," + sum_of_samples + ",#number of samples,\r\n");
                bw.write("0,0,0,#sync,\r\n");
                bw.write(",,,#sync source,\r\n");
                bw.write("g,g,g,g,\r\n");
                bw.write("--------,--------,--------,,\r\n");
            }
            for(int count=0;count<x_values.size();count++) {
                bw.write(x_values.get(count)+","+y_values.get(count)+","+z_values.get(count)+","+((sum_of_samples-x_values.size())+count)+","+time_values.get(count)+"\r\n");
            }
            if(append && iappendctr+1 >= limitappend) {
                bw.write("       ,       ,       ,,\r\n" +
                        "       ,       ,       ,,\r\n" +
                        "END,END,END,,\r\n");
                bw.write(sum_of_samples + "," + sum_of_samples + "," + sum_of_samples + ",#number of samples,\r\n");
                sum_of_samples = 0;
            }
            bw.flush();
            bw.close();
            fos.flush();
            fos.close();
            return "Success";
            /*fos.notifyAll();
            x_values.clear();
            y_values.clear();
            z_values.clear();
            file.notifyAll();
            myDir.notifyAll();*/

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return "Error";
        }

    }
}

