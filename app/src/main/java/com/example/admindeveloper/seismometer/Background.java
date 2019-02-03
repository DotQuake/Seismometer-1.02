package com.example.admindeveloper.seismometer;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
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
import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.File;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
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


        this.context = this;
        serialPortConnected = false;
        Background.SERVICE_CONNECTED = true;
        setFilter();
        usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        findSerialPortDevice();

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
        Toast.makeText(this,"Service Stopped",Toast.LENGTH_SHORT).show();
        handler.removeCallbacks(runnable);
        if(locationManager != null){
            locationManager.removeUpdates(locationListener);
        }

        unregisterReceiver(usbReceiver);
        Background.SERVICE_CONNECTED = false;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
       if (sensorEvent.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            compass = String.valueOf(sensorEvent.values[0]);
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






    //ADDED FUNCTIONS FOR USB CDC BELOW









    public static final String ACTION_USB_READY = "com.felhr.connectivityservices.USB_READY";
    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";
    public static final String ACTION_USB_NOT_SUPPORTED = "com.felhr.usbservice.USB_NOT_SUPPORTED";
    public static final String ACTION_NO_USB = "com.felhr.usbservice.NO_USB";
    public static final String ACTION_USB_PERMISSION_GRANTED = "com.felhr.usbservice.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "com.felhr.usbservice.USB_PERMISSION_NOT_GRANTED";
    public static final String ACTION_USB_DISCONNECTED = "com.felhr.usbservice.USB_DISCONNECTED";
    public static final String ACTION_CDC_DRIVER_NOT_WORKING = "com.felhr.connectivityservices.ACTION_CDC_DRIVER_NOT_WORKING";
    public static final String ACTION_USB_DEVICE_NOT_WORKING = "com.felhr.connectivityservices.ACTION_USB_DEVICE_NOT_WORKING";
    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private static final int BAUD_RATE = 9600; // BaudRate. Change this value if you need
    public static boolean SERVICE_CONNECTED = false;

    private Context context;
    private Handler mHandler;
    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private boolean serialPortConnected;

    private Integer x, y, z;

    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            if (arg1.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = arg1.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_GRANTED);
                    arg0.sendBroadcast(intent);
                    connection = usbManager.openDevice(device);
                    serialPortConnected = true;
                    new ConnectionThread().run();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    Intent intent = new Intent(ACTION_USB_PERMISSION_NOT_GRANTED);
                    arg0.sendBroadcast(intent);

                }
            } else if (arg1.getAction().equals(ACTION_USB_ATTACHED)) {
                if (!serialPortConnected)
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a Serial port
            } else if (arg1.getAction().equals(ACTION_USB_DETACHED)) {
                // Usb device was disconnected. send an intent to the Main Activity
                Intent intent = new Intent(ACTION_USB_DISCONNECTED);
                arg0.sendBroadcast(intent);
                serialPortConnected = false;
                serialPort.close();
            }
        }
    };

    private UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {

            byte[] valueX = {0x00, 0x00,arg0[1], arg0[0]};
            byte[] valueY = {0x00, 0x00,arg0[2], arg0[3]};
            byte[] valueZ = {0x00, 0x00,arg0[4], arg0[5]};

            x=ByteBuffer.wrap(valueX).getInt();
            y=ByteBuffer.wrap(valueY).getInt();
            z=ByteBuffer.wrap(valueZ).getInt();

            time = ""+ SystemClock.uptimeMillis() ;

            realTimeController.updateXYZ(x, y, z);
            recordSaveData1.recordData(realTimeController.getX(), realTimeController.getY(), realTimeController.getZ(), time);
            i.putExtra("valueX", realTimeController.getX());
            i.putExtra("valueY", realTimeController.getY());
            i.putExtra("valueZ", realTimeController.getZ());
            i.putExtra("compass",compass);
            i.setAction("FILTER");
            sendBroadcast(i);
        }
    };

    public void write(byte[] data) {
        if (serialPort != null)
            serialPort.write(data);
    }

    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                int devicePID = device.getProductId();

                if (deviceVID == 1155 && devicePID == 22336) {
                    // There is a device connected to our Android device. Try to open it as a Serial Port.
                    requestUserPermission();
                    keep = false;
                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
            if (!keep) {
                // There is no USB devices connected (but usb host were listed). Send an intent to MainActivity.
                Intent intent = new Intent(ACTION_NO_USB);
                sendBroadcast(intent);
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            Intent intent = new Intent(ACTION_NO_USB);
            sendBroadcast(intent);
        }
    }

    private void setFilter() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(ACTION_USB_DETACHED);
        filter.addAction(ACTION_USB_ATTACHED);
        registerReceiver(usbReceiver, filter);
    }

    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
    }

    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort != null) {
                if (serialPort.open()) {
                    serialPort.setBaudRate(BAUD_RATE);
                    serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                    serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                    serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                    serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                    serialPort.read(mCallback);

                    // Everything went as expected. Send an intent to MainActivity
                    Intent intent = new Intent(ACTION_USB_READY);
                    context.sendBroadcast(intent);
                } else {
                    // Serial port could not be opened, maybe an I/O error or if CDC driver was chosen, it does not really fit
                    // Send an Intent to Main Activity
                    if (serialPort instanceof CDCSerialDevice) {
                        Intent intent = new Intent(ACTION_CDC_DRIVER_NOT_WORKING);
                        context.sendBroadcast(intent);
                    } else {
                        Intent intent = new Intent(ACTION_USB_DEVICE_NOT_WORKING);
                        context.sendBroadcast(intent);
                    }
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                Intent intent = new Intent(ACTION_USB_NOT_SUPPORTED);
                context.sendBroadcast(intent);
            }
        }
    }
}

