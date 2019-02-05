package com.example.admindeveloper.seismometer;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.FileObserver;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.RealTimeServices.RealTimeController;
import com.example.admindeveloper.seismometer.UploadServices.ZipManager;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class Background extends Service implements SensorEventListener {

    private SensorManager mSensorManager;
    Bundle extras;
    Intent i;
    RecordSaveData recordSaveData1;
    RealTimeController realTimeController;
    Handler handler;
    ZipManager zipManager;
    String UPLOAD_URL;

    ArrayList<String> csvnames;
    FileObserver fileObservercsv;
    FileObserver fileObserverzip;

    boolean compressionflag = false;
    boolean append = false;
    int iappendctr = 0;
    final int limitappend = 1;

    long StartTime;
    String time;

    String fileName;

    String ipaddress;

    String longitude;
    String latitutde;
    String compass;

    private LocationManager locationManager;
    private LocationListener locationListener;

    Runnable runnable;

    @SuppressLint("MissingPermission")
    @Override
    public void onCreate() {
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                longitude = String.valueOf(location.getLongitude());
                latitutde = String.valueOf(location.getLatitude());
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);

        this.ServiceStarted=true;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            toastMessage("SERVICE: Bluetooth Not Supported");
        } else {
            IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            filter.addAction(BluetoothDevice.ACTION_FOUND);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            filter.addAction(Background.ACTION_SEND_MESSAGE);
            filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(mBroadcastReceiver, filter);

            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();
            }else{
                StartService();
            }

        }

    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId) {
        //region ---------Initialization ------------------
        StartTime = SystemClock.uptimeMillis();
        ipaddress = intent.getStringExtra("ipaddress");
        Toast.makeText(getApplication(), ipaddress, Toast.LENGTH_SHORT).show();
        csvnames = new ArrayList<>();
        zipManager = new ZipManager();
        extras = new Bundle();
        i = new Intent();
        recordSaveData1 = new RecordSaveData();
        realTimeController = new RealTimeController();
        handler = new Handler();
        //endregion
        Toast.makeText(getApplication(), "Services Enabled", Toast.LENGTH_SHORT).show();
        //region ---------------------Register Listeners for Sensors( Accelerometer / Orientation) Temporarily
        mSensorManager = (SensorManager) getSystemService(Activity.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0), SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION).get(0), SensorManager.SENSOR_DELAY_GAME);
        //endregion
        //region ------------------- Set up for Delay / Start Up --------------------
        Calendar calendar = Calendar.getInstance();                     // getting instance
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ss");       // format hour
        Date date = calendar.getTime();                             // getting current time
        final int sec = (60 - Integer.parseInt(simpleDateFormat.format(date))) * 1000; // parsing string to int
        //endregion

        //region ---------------------(HANDLER) Special Delay Call (Infinite Loop in an definite delay)--------------------

                runnable = new Runnable() {
                    @Override
                    public void run() {
                        // ------------- Se Up -----------
                        String status;
                       // Toast.makeText(getApplicationContext(), "Saving in Progress", Toast.LENGTH_SHORT).show();
                        if(iappendctr == 0 && !append) {
                            compressionflag = false;
                            Date currentTime = Calendar.getInstance().getTime();
                            fileName = (currentTime.getYear() + 1900) + "-" + (currentTime.getMonth() + 1) + "-" + currentTime.getDate() + "-" + currentTime.getHours() + "-" + currentTime.getMinutes() + "-" + currentTime.getSeconds() + ".csv";
                        }
                        // -------------- Save / Clear -------------
                        if(iappendctr+1 >= limitappend) {
                            compressionflag = true;
                        }
                            status = recordSaveData1.saveEarthquakeData("0", fileName, longitude, latitutde, compass, append , iappendctr, limitappend);      // saving Data to a specific Location (Samples)


                        Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
                        switch (status){
                            case "Success":{
                                recordSaveData1.clearData();          // deleting recorded data
                                append = true;
                                iappendctr++;
                                if(iappendctr >= limitappend) {
                                    csvnames.add(fileName);
                                    iappendctr = 0;
                                    append = false;
                                }

                                //------------------ Initialize Delay for the next Call -----------------
                                Date settime = Calendar.getInstance().getTime();
                                int secnew = (60 - settime.getSeconds()) * 1000; // seconds delay for minute
                                // ----------------- Recursive Call --------------------------
                                handler.postDelayed(this, secnew);
                                break;
                            }
                            case "Error":{
                                handler.postDelayed(this, 0);
                                break;
                            }
                            default:{
                                break;
                            }
                        }
                    }
                };
                handler.postDelayed(runnable, sec); // calling handler for infinite loop
        //endregion

        //region --------- FileObserver for Compression -------
       final String csvpath = android.os.Environment.getExternalStorageDirectory().toString() + "/Samples/";
       fileObservercsv = new FileObserver(csvpath,FileObserver.ALL_EVENTS) {
           @Override
           public void onEvent(int event, final String file) {
               if (event == FileObserver.CLOSE_WRITE && compressionflag) {
                  // Log.d("MediaListenerService", "File created [" + csvpath + file + "]");
                   new Handler(Looper.getMainLooper()).post(new Runnable() {
                       @Override
                       public void run() {
                          // Toast.makeText(getBaseContext(), file + " was saved!", Toast.LENGTH_SHORT).show();
                           zipManager.compressGzipFile("Samples/" + file,  file + ".gz");  // Compressing Data

                       }
                   });
               }
           }
       };
       fileObservercsv.startWatching();
        //endregion

        //region  -------- FileObserver for Sending Data to Database -------------
        final String zippath = android.os.Environment.getExternalStorageDirectory().toString() + "/Zip/";
        fileObserverzip = new FileObserver(zippath,FileObserver.ALL_EVENTS) {
            @Override
            public void onEvent(int event, final String file) {
                if (event == FileObserver.CREATE) {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                           // Toast.makeText(getBaseContext(), file + " was compressed!", Toast.LENGTH_SHORT).show();
                            for(int ictr=0 ; ictr<csvnames.size() ; ictr++) {
                               uploadMultipart("/storage/emulated/0/Zip/" + csvnames.get(ictr) + ".gz", csvnames.get(ictr),ictr);
                             }

                        }
                    });
                }
            }
        };
        fileObserverzip.startWatching();
        //endregion


        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        this.ServiceStarted=false;
        if(bluetoothSocket.isConnected()){
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                toastMessage("SERVICE: Cannot Close");
            }
        }
        unregisterReceiver(mBroadcastReceiver);
        Toast.makeText(this,"Service Stopped",Toast.LENGTH_SHORT).show();
        handler.removeCallbacks(runnable);
        if(locationManager != null){
            locationManager.removeUpdates(locationListener);
        }
        if(mBluetoothAdapter.isEnabled()){
            outputStream('D');
            mBluetoothAdapter.disable();
        }
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
       if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
           int deg = (int)Math.floor(sensorEvent.values[0]);
           compass=(deg+90)>360?String.valueOf(deg-270):String.valueOf(deg+90);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    public void uploadMultipart(String path, final String name, final int index) {
        //getting name for the image
        UPLOAD_URL = "http://"+ipaddress+"/data/api/uploaddata.php";
        //String name=(currentTime.getYear()+1900)+"-"+(currentTime.getMonth()+1)+"-"+currentTime.getDate()+"-"+currentTime.getHours()+currentTime.getMinutes()+"-"+currentTime.getSeconds()+".csv";
        String[] separated = name.split("-");
        String location = "Lapulapu";
        String year = separated[0];
        String month = separated[1];
        String day = separated[2];
        String hour = separated[3];
        String minute = separated[4];

        //getting the actual path of the image
        //  String path = FilePath.getPath(getActivity(), filePath);

        if (path == null) {

            Toast.makeText(this, "NULL PATH", Toast.LENGTH_LONG).show();
        } else {
            //Uploading code

            try {
                final String uploadId = UUID.randomUUID().toString();


                //Creating a multi part request
                new MultipartUploadRequest(getApplicationContext(), uploadId, UPLOAD_URL)
                        .addFileToUpload(path, "gz") //Adding file
                        .addParameter("name", name) //Adding text parameter to the request
                        .addParameter("location", location)
                        .addParameter("month", month)
                        .addParameter("day", day)
                        .addParameter("year", year)
                        .addParameter("hour", hour)
                        .addParameter("minute", minute)
                        .setNotificationConfig(new UploadNotificationConfig())
                        .setMaxRetries(2)
                        .setDelegate(new UploadStatusDelegate() {
                            @Override
                            public void onProgress(Context context, UploadInfo uploadInfo) {
                                Toast.makeText(getApplicationContext(), "Uploading to Server", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
                                Toast.makeText(getApplicationContext(), "Server Connection Failed", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                                Toast.makeText(getApplicationContext(), serverResponse.getBodyAsString(), Toast.LENGTH_SHORT).show();
                                if(serverResponse.getBodyAsString().equals("Successfully Uploaded yehey")) {
                                    File file1 = new File("/storage/emulated/0/Samples/", name);
                                    boolean deleted1 = file1.delete();
                                    File file2 = new File("/storage/emulated/0/Zip/", name + ".gz");
                                    boolean deleted2 = file2.delete();
                                    csvnames.remove(index);
                                }

                            }

                            @Override
                            public void onCancelled(Context context, UploadInfo uploadInfo) {
                                Toast.makeText(getApplicationContext(), "Uploading Cancelled", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .startUpload(); //Starting the upload

            } catch (Exception exc) {
                Toast.makeText(getApplicationContext(), exc.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }


    //BLUETOOTH


    private BluetoothAdapter mBluetoothAdapter;
    private String HC06DeviceName="HC-06";
    private String HC06DeviceAddress;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    public static boolean ServiceStarted=false;

    private boolean calibrationFlag=false;
    private Integer calibrateX=0,calibrateY=0,calibrateZ=0;
    private final int maxSamples=100;

    public static String ACTION_START_SERVICE  =  "com.dotquake.bluetoothservice.ACTION_START_SERVICE";
    public static String ACTION_STOP_SERVICE  =  "com.dotquake.bluetoothservice.ACTION_STOP_SERVICE";
    public static String ACTION_REQUEST_BT="com.dotquake.bluetoothservice.ACTION_REQUEST_BT";
    public static String ACTION_DATA_READY="com.dotquake.bluetoothservice.ACTION_DATA_READY";
    public static String ACTION_SEND_MESSAGE="com.dotquake.bluetoothservice.ACTION_SEND_MESSAGE";

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {

                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch(mode){
                    case BluetoothAdapter.STATE_ON: {
                        toastMessage("SERVICE: Bluetooth On");
                        StartService();
                        break;
                    }
                    case BluetoothAdapter.STATE_TURNING_ON: {
                        mBluetoothAdapter.enable();
                        toastMessage("SERVICE: Bluetooth Turning On");
                        break;
                    }
                    case BluetoothAdapter.STATE_OFF: {
                        toastMessage("SERVICE: Bluetooth Turning Off");
                        break;
                    }
                }
            }else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (device.getName().equals(HC06DeviceName)) {
                        mBluetoothAdapter.cancelDiscovery();
                        toastMessage("SERVICE: Device Detected");
                        bluetoothDevice = device;
                        InitializeConnection(bluetoothDevice);
                    }
                }catch(Exception e){}
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                toastMessage("SERVICE: Discovering");
            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
                toastMessage("SERVICE: Done Discovering");
            }else if(Background.ACTION_SEND_MESSAGE.equals(action)){
                char command=intent.getCharExtra("command",'P');
                if(command=='P'){
                    outputStream('P');
                }else{
                    outputStream('D');
                }
            }
        }
    };

    private void toastMessage(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }

    private void StartService() {
        bluetoothDevice=null;
        if(!QueryDevice())
        {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try{
                        if (mBluetoothAdapter.isDiscovering()) {
                            mBluetoothAdapter.cancelDiscovery();
                        }
                        mBluetoothAdapter.startDiscovery();
                        while(bluetoothDevice==null){
                            if(!mBluetoothAdapter.isDiscovering()){
                                mBluetoothAdapter.startDiscovery();
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        toastMessage("SERVICE: Error Discovery");
                    }
                }
            }).start();
        }else{
            InitializeConnection(bluetoothDevice);
        }
        mInputStream=null;
        mOutputStream=null;
        try {
            mInputStream=bluetoothSocket.getInputStream();
        } catch (IOException e) {
            toastMessage("SERVICE: Failed to get InputStream");
        }
        try {
            mOutputStream=bluetoothSocket.getOutputStream();
        } catch (IOException e) {
            toastMessage("SERVICE: Failed to get OutputStream");
        }
        if(mOutputStream!=null&&mInputStream!=null){
            outputStream('D');
            InputStream();
        }
    }
    private boolean QueryDevice() {
        try {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(HC06DeviceName)) {
                        bluetoothDevice = device;
                        return true;
                    }
                }
            }
        }catch(Exception e){
            toastMessage("SERVICE: Query Error");
        }
        return false;
    }
    private boolean InitializeConnection(BluetoothDevice device) {
        Toast.makeText(getApplicationContext(), "SERVICE: Now Initializing", Toast.LENGTH_SHORT).show();
        try {
            // Get a BluetoothSocket to connect with the given BluetoothDevice.
            // MY_UUID is the app's UUID string, also used in the server code.
            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            toastMessage("SERVICE: RFCOMM Establish");
        }
        catch (IOException e) {
            toastMessage("SERVICE: Connection init failed");
        }
        try {
            // Connect to the remote device through the socket. This call blocks
            // until it succeeds or throws an exception.
            bluetoothSocket.connect();
            toastMessage("SERVICE: Success");
            return true;
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                toastMessage("SERVICE: Connect Fail");
                bluetoothSocket.close();
            } catch (IOException closeException) {
                toastMessage("SERVICE: Cannot closed");
            }
        }
        return false;
    }
    //Careful of using this function, this will create a permanent while loop if wrong command or size has been inputted
    private void InputStream()
    {
        new Thread(new Runnable() {
            private Short byteToShort(byte[] value)
            {
                ByteBuffer wrapper=ByteBuffer.wrap(value);
                return wrapper.getShort();
            }
            @Override
            public void run() {
                byte[] mmBuffer=new byte[6];
                Short x;
                Short y;
                Short z;
                int countStartingSamples=1;
                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                        // Read from the InputStream.
                        while(mInputStream.available()>=6) {
                            mInputStream.read(mmBuffer);
                            byte[] valueX = {mmBuffer[1], mmBuffer[0]};
                            byte[] valueY = {mmBuffer[3], mmBuffer[2]};
                            byte[] valueZ = {mmBuffer[5], mmBuffer[4]};
                            x = byteToShort(valueX);
                            y = byteToShort(valueY);
                            z = byteToShort(valueZ);
                            if(calibrationFlag==true) {
                                time = "" + SystemClock.uptimeMillis();

                                realTimeController.updateXYZ(x, y, z);
                                recordSaveData1.recordData(realTimeController.getX(), realTimeController.getY(), realTimeController.getZ(), time);
                                i.putExtra("valueX", x);
                                i.putExtra("valueY", y);
                                i.putExtra("valueZ", z);
                                i.putExtra("compass", compass);
                                i.setAction("FILTER");
                                sendBroadcast(i);
                            }else{
                                if(countStartingSamples<maxSamples){
                                    calibrateX+=Integer.parseInt(String.valueOf(x));
                                    calibrateY+=Integer.parseInt(String.valueOf(y));
                                    calibrateZ+=Integer.parseInt(String.valueOf(z));
                                    countStartingSamples++;
                                }else{
                                    calibrationFlag=true;
                                    calibrateX/=countStartingSamples;
                                    calibrateY/=countStartingSamples;
                                    calibrateZ/=countStartingSamples;
                                    realTimeController.setCalibrationValue(calibrateX,calibrateY,calibrateZ);
                                }
                            }
                        }
                        // Send the obtained bytes to the UI activity.
                    } catch (IOException e) {
                        //toastMessage("SERVICE: Input stream was disconnected");
                        break;
                    }
                }
            }
        }).start();
    }

    private void outputStream(char command){
        try {
            mOutputStream.write(command);
        } catch (IOException e) {
            toastMessage("SERVICE: Failed to send");
        }
    }


}

