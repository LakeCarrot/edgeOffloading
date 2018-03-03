package com.example.bohu.edgeoffloading;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.widget.Toast.makeText;

// face recognition related lib



public class MainActivity extends AppCompatActivity {
    static {
        System.loadLibrary("opencv_java3");
    }

    /**
     * Scheduler-related variables
     */
    private String destination;
    private static int APPPORT = 50053;  // tcp port which the offloading app will listen to

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Map<String, String> schedulerTrans = new HashMap<>();
        schedulerTrans.put("34.218.97.178", "m1");
        schedulerTrans.put("52.32.37.78", "m2");
        schedulerTrans.put("34.210.236.180", "m3");
        schedulerTrans.put("35.162.89.207", "m4");
        schedulerTrans.put("34.215.123.179", "m5");
        schedulerTrans.put("34.218.85.62", "m6");
        schedulerTrans.put("34.218.40.221", "m7");
        schedulerTrans.put("54.70.118.25", "m8");
        schedulerTrans.put("34.215.4.4", "m9");
        schedulerTrans.put("34.212.255.112", "m10");
        schedulerTrans.put("34.218.34.145", "m11");
        schedulerTrans.put("34.212.158.200", "m12");
        schedulerTrans.put("35.165.231.66", "m13");
        schedulerTrans.put("35.160.178.233", "m14");
        schedulerTrans.put("35.162.173.174", "m15");
        schedulerTrans.put("52.89.98.213", "m16");
        schedulerTrans.put("52.32.48.185", "m17");
        schedulerTrans.put("52.39.84.224", "m18");
        schedulerTrans.put("34.218.107.169", "m19");
        schedulerTrans.put("54.187.129.27", "m20");


        Button button = (Button) findViewById(R.id.remoteGprc);
        button.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            public void onClick(View v) {
                // slave1
                executor.execute(new MultiSender("34.218.103.6", 50052));
            }
        });
        Button button2 = (Button) findViewById(R.id.speech_slave1);
        button2.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();

            public void onClick(View v) {
                for (Map.Entry<String, String> entry : schedulerTrans.entrySet()) {
                    Log.e("Rui", "[speech] connect to " + entry.getValue());
                    executor.execute(new DirectSpeech(entry.getKey(), 50052));
                    //executor.execute(new DirectOCR("34.218.97.178", 50055));
                }
            }
        });
        Button button3 = (Button) findViewById(R.id.speech_slave2);
        button3.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();

            public void onClick(View v) {
                executor.execute(new DirectSpeech("172.28.140.65", 50052));
            }
        });
        Button button4 = (Button) findViewById(R.id.speech_slave3);
        button4.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();

            public void onClick(View v) {
                executor.execute(new DirectSpeech("172.28.142.226", 50052));
            }
        });
        Button button5 = (Button) findViewById(R.id.speech_master);
        button5.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();

            public void onClick(View v) {
                executor.execute(new DirectSpeech("172.28.136.3", 50052));
            }
        });
        Button button6 = (Button) findViewById(R.id.ocr_slave1);
        button6.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();

            public void onClick(View v) {
                //executor.execute(new DirectOCR("172.28.142.176", 40051));
                executor.execute(new DirectOCR("34.218.97.178", 40051));
            }
        });
        Button button7 = (Button) findViewById(R.id.ocr_slave2);
        button7.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();

            public void onClick(View v) {
                executor.execute(new DirectOCR("172.28.140.65", 40051));
            }
        });
        Button button8 = (Button) findViewById(R.id.ocr_slave3);
        button8.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();

            public void onClick(View v) {
                executor.execute(new DirectOCR("172.28.142.226", 40051));
            }
        });
        Button button9 = (Button) findViewById(R.id.ocr_master);
        button9.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();

            public void onClick(View v) {
                executor.execute(new DirectOCR("172.28.136.3", 40051));
            }
        });

    }
}
