package com.example.admindeveloper.seismometer.UploadServices;

import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;

public class ZipManager {


   /* public void decompressGzipFile(String gzipFile, String newFile) {
        try {
            File current_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),gzipFile);
            File new_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),newFile);
            FileInputStream fis = new FileInputStream(current_file);
            GZIPInputStream gis = new GZIPInputStream(fis);
            FileOutputStream fos = new FileOutputStream(new_file);
            byte[] buffer = new byte[1024];
            int len;
            while((len = gis.read(buffer)) != -1){
                fos.write(buffer, 0, len);
            }
            //close resources
            fos.close();
            gis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }*/

    public Boolean compressGzipFile(String fileLocation, String gzipFileLocation) {
        try {
            File myDir = new File("storage/emulated/0/Zip");
            if(!myDir.exists())
            {
                myDir.mkdirs();
            }
            File current_file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),fileLocation);
            File compressed_file = new File(myDir,gzipFileLocation);
            compressed_file.createNewFile();

            FileInputStream fis = new FileInputStream(current_file);
            FileOutputStream fos = new FileOutputStream(compressed_file);
            GZIPOutputStream gzipOS = new GZIPOutputStream(fos);
            byte[] buffer = new byte[1024];
            int len;
            while((len=fis.read(buffer)) != -1){
                gzipOS.write(buffer, 0, len);
            }
            //close resources
            gzipOS.close();
            fos.close();
            fis.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;

    }


   /* public void backupgzip() {
        byte[] buffer = new byte[1024];
        String fileName =  "test.gzip";
        File file1 = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),fileName);


        try {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"aw.txt");

            FileOutputStream fileOutputStream =new FileOutputStream(file1);
            Log.d("file","fileoutput");
            GZIPOutputStream gzipOuputStream = new GZIPOutputStream(fileOutputStream);
            Log.d("file","gzip");
            FileInputStream fileInput = new FileInputStream(file);
            Log.d("file","fileinput");
            int bytes_read;

            while ((bytes_read = fileInput.read(buffer)) > 0) {
                gzipOuputStream.write(buffer, 0, bytes_read);
            }

            fileInput.close();

            gzipOuputStream.finish();
            gzipOuputStream.close();


        } catch (IOException ex) {
            ex.printStackTrace();
            Log.d("file","error");
        }

    }*/

}