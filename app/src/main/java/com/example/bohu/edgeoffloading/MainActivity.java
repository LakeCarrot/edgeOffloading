package com.example.bohu.edgeoffloading;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edgeOffloading.OffloadingGrpc;
import edgeOffloading.OffloadingOuterClass;
import faceRecognition.FacerecognitionGrpc;
import faceRecognition.FacerecognitionOuterClass.FaceRecognitionRequest;
import faceRecognition.FacerecognitionOuterClass.FaceRecognitionReply;
import edgeOffloading.OffloadingOuterClass.OffloadingRequest;
import edgeOffloading.OffloadingOuterClass.OffloadingReply;
import io.grpc.stub.StreamObserver;
import speechRecognition.SpeechrecognitionGrpc;
import speechRecognition.SpeechrecognitionOuterClass.SpeechRecognitionRequest;
import speechRecognition.SpeechrecognitionOuterClass.SpeechRecognitionReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import static android.widget.Toast.makeText;
import static org.opencv.core.CvType.CV_32SC1;

// face recognition related lib



public class MainActivity extends AppCompatActivity {
    static{ System.loadLibrary("opencv_java3"); }

    /**
     * Scheduler-related variables
     */
    private String destination;
    private static int APPPORT = 50053;  // tcp port which the offloading app will listen to

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * Face recognition (start)
         */
        // Check the I/O permission before doing any other operations
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            Log.d("main", "we don't have the write permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
            }
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    2);

            return;
        }
        /**
         * Face recognition (end)
         */
        /*
        Button button=(Button) findViewById(R.id.remoteGprc);
        button.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                int numConcurrentApps = 6;
                ExecutorService executor = Executors.newCachedThreadPool();
                for (int i = 0; i < numConcurrentApps; i++) {
                    // the app will always send offloading requests to slave1
                    executor.execute(new MultiSender("172.28.142.176", APPPORT));

                    try {
                        Thread.sleep(5000);
                    } catch (Exception e) {
                        new Exception().printStackTrace();
                    }


                    APPPORT++;
                }
            }
        });
        */
        Button button=(Button) findViewById(R.id.remoteGprc);
        button.setOnClickListener(new View.OnClickListener(){
            ExecutorService executor = Executors.newCachedThreadPool();
            public void onClick(View v){
                // slave1
                executor.execute(new MultiSender("172.28.142.176", 50052));
                // master
                executor.execute(new MultiSender("172.28.143.136", 50052));
                // slave2
                executor.execute(new MultiSender("172.28.140.65", 50052));
                // slave3
                executor.execute(new MultiSender("172.28.142.226", 50052));
            }
        });

        Button button2=(Button) findViewById(R.id.remoteSpeech);
        button2.setOnClickListener(new View.OnClickListener(){
            ExecutorService executor = Executors.newCachedThreadPool();
            public void onClick(View v){
                executor.execute(new MultiSender("172.28.142.176", 50052));
            }
        });
        //new LocalSpeechRecognition(this).execute();
        //new RemoteSpeechRecognition().execute();
        //faceRecognition();
        //new FaceTask().execute();
    }

    private void faceRecognition() {
        String trainingDir = "/trainFace/";
        Log.e("Rui","s11");
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, "/testFace/"+ 1 + ".jpg");
        Mat testImage = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        if(testImage.empty()) {
            Log.d("image", "wrong!!!");
        }
        File trainImages = new File(root, trainingDir);
        FilenameFilter imgFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            }
        };
        Log.e("Rui","s22");
        File[] imageFiles =  trainImages.listFiles(imgFilter);
        List<Mat> images = new ArrayList<>(imageFiles.length);
        int[] labelsBuf =  new int[imageFiles.length];
        int counter = 0;
        for(File image : imageFiles) {
            Mat img = Imgcodecs.imread(image.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
            int label = Integer.parseInt(image.getName().split("\\-")[0]);
            images.add(img);
            labelsBuf[counter] = label;
            counter++;
        }
        Log.e("Rui","start to train");
        FaceRecognizer face = LBPHFaceRecognizer.create();
        face.train(images, new MatOfInt(labelsBuf));


        long overallTimeStart = System.currentTimeMillis();
        File folder = new File(root, "/testFace/");
        File[] listOfFiles = folder.listFiles();
        Log.e("Rui","start to test");
        for (int i = 0; i < listOfFiles.length; i++) {
            Log.e("Rui", Integer.toString(i));
            file = new File(root, "/testFace/"+ listOfFiles[i].getName());
            testImage = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
            int[] label = new int[1];
            double[] confidence = new double[1];
            face.predict(testImage, label, confidence);
            int predictedLabel = label[0];
        }
        long overallTimeEnd = System.currentTimeMillis();
        Log.e("Rui","overall processing time " + (overallTimeEnd - overallTimeStart));
    }


}
