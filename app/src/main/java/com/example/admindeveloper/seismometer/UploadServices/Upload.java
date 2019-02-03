package com.example.admindeveloper.seismometer.UploadServices;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admindeveloper.seismometer.R;

import net.gotev.uploadservice.MultipartUploadRequest;
import net.gotev.uploadservice.ServerResponse;
import net.gotev.uploadservice.UploadInfo;
import net.gotev.uploadservice.UploadNotificationConfig;
import net.gotev.uploadservice.UploadStatusDelegate;

import java.io.File;
import java.util.UUID;


public class Upload extends Fragment implements View.OnClickListener {
    View myView;

    //Declaring views
    private Button buttonChoose;
    private Button buttonUpload;
    private Button buttonZip;
    private Button buttonDelete;
    private Button buttonStart;
    private Button buttonStop;

    private EditText editText , ipadress;
    private TextView timertv;
    Handler handler;
    public String UPLOAD_URL ;


    //Csv request code
    private int PICK_CSV_REQUEST = 1;

    //storage permission code
    private static final int STORAGE_PERMISSION_CODE = 123;


    //Uri to store the image uri
    private Uri filePath;

    ZipManager zipManager = new ZipManager();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        myView = inflater.inflate(R.layout.uploadlayout,container,false);
        //Requesting storage permission
        requestStoragePermission();

        //Initializing views
        buttonChoose =  myView.findViewById(R.id.buttonChoose);
        buttonUpload =  myView.findViewById(R.id.buttonUpload);
        buttonZip = myView.findViewById(R.id.buttonzip);
        buttonDelete =  myView.findViewById(R.id.buttondelete);
        buttonStart = myView.findViewById(R.id.buttonstart);
        buttonStop = myView.findViewById(R.id.buttonstop);
        handler = new Handler();

        timertv = myView.findViewById(R.id.stopwatchtv);

        editText = (EditText) myView.findViewById(R.id.editTextName);
        ipadress = myView.findViewById(R.id.ipaddress);


        //Setting clicklistener
        buttonStop.setOnClickListener(this);
        buttonStart.setOnClickListener(this);
        buttonChoose.setOnClickListener(this);
        buttonUpload.setOnClickListener(this);
        buttonZip.setOnClickListener(this);
        buttonDelete.setOnClickListener(this);
        return myView;
    }
    public void uploadMultipart() {
        //getting name for the image
        UPLOAD_URL =  "http://"+ipadress.getText().toString()+"/data/api/uploaddata.php";
        String name = editText.getText().toString().trim();
        String location = "Lapulapu";
        String month = "12";
        String day = "25";
        String year = "2018";
        String hour = "12";
        String minute = "00";

        //getting the actual path of the image
        String path = FilePath.getPath(getActivity(), filePath);
        Toast.makeText(getActivity(),path,Toast.LENGTH_SHORT).show();

        if (path == null) {

            //Toast.makeText(this, "Please move your .csv file to internal storage and retry", Toast.LENGTH_LONG).show();
        } else {
            //Uploading code
            try {
                final String uploadId = UUID.randomUUID().toString();

                //Creating a multi part request
                new MultipartUploadRequest(getActivity(), uploadId, UPLOAD_URL)
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
                                Toast.makeText(getActivity(), "Uploading to Server", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Context context, UploadInfo uploadInfo, ServerResponse serverResponse, Exception exception) {
                                Toast.makeText(getActivity(), "Server Connection Failed", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCompleted(Context context, UploadInfo uploadInfo, ServerResponse serverResponse) {
                                Toast.makeText(getActivity(), serverResponse.getBodyAsString(), Toast.LENGTH_SHORT).show();


                            }

                            @Override
                            public void onCancelled(Context context, UploadInfo uploadInfo) {
                                Toast.makeText(getActivity(), "Uploading Cancelled", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .startUpload(); //Starting the upload

            } catch (Exception exc) {
                Toast.makeText(getActivity(), exc.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }
    //method to show file chooser
    //intent.setType("application/zip");
    private void showFileChooser() {
        Intent intent = new Intent();
        intent.setType("application/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select csv"), PICK_CSV_REQUEST);
    }

    //handling the image chooser activity result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CSV_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            filePath = data.getData();
        }
    }

    //Requesting permission
    private void requestStoragePermission() {
       /* int permission = ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(getActivity(),new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},12);
        }
    */
        if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1000);
        }

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){

            return;
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
        }
        //And finally ask for the permission
        ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
    }


    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        //Checking the request code of our request
        if (requestCode == STORAGE_PERMISSION_CODE) {

            //If permission is granted
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //Displaying a toast
                Toast.makeText(getActivity(), "Permission granted now you can read the storage", Toast.LENGTH_LONG).show();
            } else {
                //Displaying another toast if permission is not granted
                Toast.makeText(getActivity(), "Oops you just denied the permission", Toast.LENGTH_LONG).show();
            }
        }
    }

    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    int Seconds, Minutes, MilliSeconds ;
    public Runnable runnable = new Runnable() {

        public void run() {

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            timertv.setText( "" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds) );

            handler.postDelayed(this, 0);
        }

    };

    @Override
    public void onClick(View v) {
        if (v == buttonStart) {
            StartTime = SystemClock.uptimeMillis();
            handler.postDelayed(runnable, 0);
        }
        if (v == buttonStop) {
            TimeBuff += MillisecondTime;
            handler.removeCallbacks(runnable);
        }
        if (v == buttonChoose) {
            showFileChooser();
        }
        if (v == buttonUpload) {
            uploadMultipart();
        }
        if(v == buttonZip){
           // File file = new File("/storage/emulated/0/");
           // File file1 = Environment.getExternalStorageDirectory();
            zipManager.compressGzipFile("aw.txt","Samples/newest.txt.gz");
           // Toast.makeText(getActivity(),String.valueOf( file.getFreeSpace()),Toast.LENGTH_SHORT).show();
           // Toast.makeText(getActivity(),String.valueOf( file1),Toast.LENGTH_SHORT).show();
            /*if(isExternalStorageWritable()){
                Toast.makeText(getActivity(),"Writable",Toast.LENGTH_SHORT).show();
            }
            if(isExternalStorageReadable()){
                Toast.makeText(getActivity(),"Readable",Toast.LENGTH_SHORT).show();
            }*/
            String sdpath,sd1path,usbdiskpath,sd0path;

            if(new File("/storage/extSdCard/").exists())
            {sdpath="/storage/extSdCard/";
                Toast.makeText(getActivity(),sdpath,Toast.LENGTH_SHORT).show();}

            if(new File("/storage/sdcard1/").exists())
            {sd1path="/storage/sdcard1/";
                Toast.makeText(getActivity(),sd1path,Toast.LENGTH_SHORT).show();}

            if(new File("/storage/usbcard1/").exists())
            {usbdiskpath="/storage/usbcard1/";
                Toast.makeText(getActivity(),usbdiskpath,Toast.LENGTH_SHORT).show();}

            if(new File("/storage/sdcard0/").exists())
            {sd0path="/storage/sdcard0/";
                Toast.makeText(getActivity(),sd0path,Toast.LENGTH_SHORT).show();}

        }
        if(v == buttonDelete){
            //File dir = "/storage/emulated/0/Zip";;
            File file = new File("/storage/emulated/0/Zip/","2019-1-27-11-22-0.csv.gz");
            boolean deleted = file.delete();
            /*boolean isSDPresent = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            boolean isSDSupported = Environment.isExternalStorageRemovable();
            if(isSDPresent && isSDSupported){
                Toast.makeText(getActivity(),"SD Card Present",Toast.LENGTH_SHORT).show();
            }else
            {
                Toast.makeText(getActivity(),"No SD Card",Toast.LENGTH_SHORT).show();
            }*/
        }
    }
    //region SDcard Path
    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public String getExternalStoragePath() {
        String removableStoragePath="";
        File fileList[] = new File("/storage/").listFiles();
        for (File file : fileList)
        {     if(!file.getAbsolutePath().equalsIgnoreCase(Environment.getExternalStorageDirectory().getAbsolutePath()) && file.isDirectory() && file.canRead())
            removableStoragePath = file.getAbsolutePath();  }

            return removableStoragePath;
        /*String internalPath = Environment.getExternalStorageDirectory().getAbsolutePath();
        String[] paths = internalPath.split("/");
        String parentPath = "/";
        for (String s : paths) {
            if (s.trim().length() > 0) {
                parentPath = parentPath.concat(s);
                break;
            }
        }
        File parent = new File(parentPath);
        if (parent.exists()) {
            File[] files = parent.listFiles();
            for (File file : files) {
                String filePath = file.getAbsolutePath();
               // Log.d(TAG, filePath);
                if (filePath.equals(internalPath)) {
                    continue;
                } else if (filePath.toLowerCase().contains("sdcard")) {
                    return filePath;
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    try {
                        if (Environment.isExternalStorageRemovable(file)) {
                            return filePath;
                        }
                    } catch (RuntimeException e) {
                        //Log.e(TAG, "RuntimeException: " + e);
                    }
                }
            }

        }
        return null;*/
    }
//endregion


}

