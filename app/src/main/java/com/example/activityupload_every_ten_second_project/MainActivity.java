package com.example.activityupload_every_ten_second_project;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;

import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.LongDef;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    int serverResponseCode = 0;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Context mContext;

    private DataOutputStream out_acc;

    private boolean startRec = false;

    private Calendar calendar;

    private float[] gravityValues = null;
    private float[] magneticValues = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        if (Build.VERSION.SDK_INT >= 23) {
//            int REQUEST_CODE_PERMISSION_STORAGE = 100;
//            String[] permissions = {
//                    Manifest.permission.READ_EXTERNAL_STORAGE,
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE
//            };
//
//            for (String str : permissions) {
//                if (this.checkSelfPermission(str) != PackageManager.PERMISSION_GRANTED) {
//                    this.requestPermissions(permissions, REQUEST_CODE_PERMISSION_STORAGE);
//                    return;
//                }
//            }
//        }

        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
                ||ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED)//檢查是否開啟該應用程式的位置權限
        {
            request_permissions();//沒有開啟的話就在跳出開啟該應用程式位置權限視窗
//            Log.d("request_permissions", "request_permissions");
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);

        // Show all supported sensor
//        for(int i = 0; i < deviceSensors.size(); i++) {
//            Log.d("[SO]", deviceSensors.get(i).getName());
//        }

        if ((mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)) != null){
            mSensorManager.registerListener(mSensorListener, mSensor, 100);
        } else {
            Toast.makeText(mContext, "ACCELEROMETER is not supported!", Toast.LENGTH_SHORT).show();
        }

        if ((mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)) != null){
            mSensorManager.registerListener(mSensorListener, mSensor, 100);
        } else {
            Toast.makeText(mContext, "GRAVITY is not supported!", Toast.LENGTH_SHORT).show();
        }

        if ((mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) != null){
            mSensorManager.registerListener(mSensorListener, mSensor, 100);
        } else {
            Toast.makeText(mContext, "MAGNETOMETER is not supported!", Toast.LENGTH_SHORT).show();
        }

        Thread t1=new Thread(r1);
        t1.start();
    }

    private Runnable r1=new Runnable () {
        SimpleDateFormat formatter = new SimpleDateFormat("ss");
        int pre_f = -1;
        int cur_f = -1;
        final Handler handler = new Handler();
        public void run() {
            while (true) {
//                //原本對時的方法
//                Date curDate = new Date(System.currentTimeMillis()); // 獲取當前時間
//                cur_f = Integer.valueOf(formatter.format(curDate));

                //更新對時的方法
                Date curDate = new Date(System.currentTimeMillis()); // 獲取當前時間
                cur_f = Integer.valueOf(formatter.format(curDate));
                if (cur_f != pre_f) {
                    String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                    String dir_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AbsAccCollection";
//                    String dir_path = "data/data/com.example.activityupload_every_ten_second_project";
                    File dir = new File(dir_path);
                    Log.d("Current_1",""+cur_f);
                    Log.d("Preview_1",""+pre_f);
                    final int pre_1 = pre_f;
                    if(!dir.exists()) {
                        dir.mkdir();
                    }
                    File file_acc = new File(dir, androidId+"_"+cur_f+".txt");
                    Log.d("Current",""+cur_f);
                    try {
                        file_acc.createNewFile();
                        Log.d("FILE",dir_path+" "+androidId+"_"+cur_f+".txt");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out_acc = new DataOutputStream(new FileOutputStream(file_acc, true));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    handler.post(new Runnable() {
                        public void run() {
                            try {
                                Log.d("Current_2",""+cur_f);
                                UploadFileAsync performBackgroundTask = new UploadFileAsync();
                                // PerformBackgroundTask this class is the class that extends AsynchTask
                                performBackgroundTask.execute(String.valueOf(pre_1));
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                            }
                        }
                    });

                }
                pre_f = cur_f;
            }
        }
    };
    private class UploadFileAsync extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            try {
                String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
                String dir_path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/AbsAccCollection/"+androidId+"_"+Integer.valueOf(params[0])+".txt";

                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile = new File(dir_path);
                //Log.d("File_Path",sourceFile.getAbsolutePath());
                if (sourceFile.isFile()) {

                    try {
//                        Log.d("Uploading_File.....",sourceFile.getName());
                        String upLoadServerUri = "http://192.168.0.189/abc.php?";
                        // open a URL connection to the Servlet
                        FileInputStream fileInputStream = new FileInputStream(sourceFile);
                        URL url = new URL(upLoadServerUri);

                        // Open a HTTP connection to the URL
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setDoInput(true); // Allow Inputs
                        conn.setDoOutput(true); // Allow Outputs
                        conn.setUseCaches(false); // Don't use a Cached Copy
                        conn.setRequestMethod("POST");
                        conn.setRequestProperty("Connection", "Keep-Alive");
                        conn.setRequestProperty("ENCTYPE",
                                "multipart/form-data");
                        conn.setRequestProperty("Content-Type",
                                "multipart/form-data;boundary=" + boundary);
                        conn.setRequestProperty("bill", dir_path);

                        dos = new DataOutputStream(conn.getOutputStream());

                        dos.writeBytes(twoHyphens + boundary + lineEnd);
                        dos.writeBytes("Content-Disposition: form-data; name=\"bill\";filename=\""
                                + dir_path + "\"" + lineEnd);

                        dos.writeBytes(lineEnd);

                        // create a buffer of maximum size
                        bytesAvailable = fileInputStream.available();

                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        buffer = new byte[bufferSize];

                        // read file and write it into form...
                        bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        while (bytesRead > 0) {

                            dos.write(buffer, 0, bufferSize);
                            bytesAvailable = fileInputStream.available();
                            bufferSize = Math.min(bytesAvailable, maxBufferSize);
                            bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                        }

                        // send multipart form data necesssary after file
                        // data...
                        dos.writeBytes(lineEnd);
                        dos.writeBytes(twoHyphens + boundary + twoHyphens
                                + lineEnd);

                        // Responses from the server (code and message)
                        serverResponseCode = conn.getResponseCode();
                        String serverResponseMessage = conn
                                .getResponseMessage();

                        if (serverResponseCode == 200) {

                        }
//                        if(serverResponseMessage.equals("success")){
//                            //Success! Do whatever
//                            Log.d("file access", String.valueOf(serverResponseMessage));
//                        } else {
//                            Log.d("file not access", String.valueOf(serverResponseMessage));
//                            //In your script, it fails if it goes here
//                        }

                        // close the streams //
                        fileInputStream.close();
                        dos.flush();
                        dos.close();

                        if (sourceFile.exists()) {
                            sourceFile.delete();
                        }

                    } catch (Exception e) {

                        // dialog.dismiss();
                        e.printStackTrace();
//                        Log.d("Uploading_Fail","Uploading_File_Fail.....");
                    }
                    // dialog.dismiss();

                } // End else block


            } catch (Exception ex) {
                // dialog.dismiss();
//                Log.d("Uploading_Fail","Uploading_File_Fail.....");
                ex.printStackTrace();
            }
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }


    private SensorEventListener mSensorListener = new SensorEventListener(){

        public final void onSensorChanged(SensorEvent event) {
            String raw;
            String time;
            if ((gravityValues != null) && (magneticValues != null)
                    && (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION)) {
                try {

                    float[] deviceRelativeAcceleration = new float[4];
                    deviceRelativeAcceleration[0] = event.values[0];
                    deviceRelativeAcceleration[1] = event.values[1];
                    deviceRelativeAcceleration[2] = event.values[2];
                    deviceRelativeAcceleration[3] = 0;

                    // Change the device relative acceleration values to earth relative values
                    // X axis -> East
                    // Y axis -> North Pole
                    // Z axis -> Sky

                    float[] R = new float[16], I = new float[16], earthAcc = new float[16];

                    SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

                    float[] inv = new float[16];

                    android.opengl.Matrix.invertM(inv, 0, R, 0);
                    android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);


                    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
                    Date curDate = new Date(System.currentTimeMillis()); // 獲取當前時間
                    time = formatter.format(curDate);
                    raw = String.valueOf(earthAcc[0]) + "," + String.valueOf(earthAcc[1]) + "," + String.valueOf(earthAcc[2]) + ","
                            + time + "\n";
//                    Log.d("RAW 資料",raw);
                    out_acc.write(raw.getBytes());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
                gravityValues = event.values;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticValues = event.values;
            }

        }

        @Override
        public final void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something here if sensor accuracy changes.
        }
    };

    private void request_permissions()//檢查權限
    {
        // 創建一個權限列表，把需要使用而沒有授權的的權限存放在這裡
        List<String> permissionList = new ArrayList<>();

        // 判斷權限是否已經授予，没有就把該權限添加到列表中
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        // 如果列表為空，代表全部權限都有了，反之則代表有權限還沒有被加到list，要申请權限
        if (!permissionList.isEmpty())//列表不是空的
        {
            ActivityCompat.requestPermissions(this,permissionList.toArray(new String[permissionList.size()]), 1002);
        }
    }
    // 請求權限回調方法
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(mSensorListener);
//        new UploadFileAsync();
    }
    protected void onDestroy() {
        super.onDestroy();
        // cancel timer
    }

}
