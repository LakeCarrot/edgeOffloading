package com.example.bohu.edgeoffloading;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edgeOffloading.OffloadingGrpc;
import edgeOffloading.OffloadingOuterClass;
import faceRecognition.FacerecognitionGrpc;
import faceRecognition.FacerecognitionOuterClass.FaceRecognitionRequest;
import faceRecognition.FacerecognitionOuterClass.FaceRecognitionReply;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import static org.opencv.core.CvType.CV_32SC1;

// face recognition related lib



public class MainActivity extends AppCompatActivity {
    static{ System.loadLibrary("opencv_java3"); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Check the I/O permission before doing any other operations
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            Log.d("main", "we don't have the write permission");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (shouldShowRequestPermissionRationale(
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {
                // Explain to the user why we need to read the contacts
            }

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    2);

            return;
        }
        faceRecognition();
        ///new GrpcTask().execute();
        //new FaceTask().execute();

    }

    private class GrpcTask extends AsyncTask<Void, Void, String> {
        private ManagedChannel mChannel;
        private String hostIP;
        private int hostPort;

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                // first version use static IP and port
                hostIP = "172.28.142.176";
                hostPort = 50052;
                mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                        .usePlaintext(true)
                        .build();
                OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(mChannel);
                OffloadingOuterClass.OffloadingRequest message = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage("Who are you?").build();
                OffloadingOuterClass.OffloadingReply reply = stub.startService(message);
                return reply.getMessage();
            } catch(Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return String.format("Failed... : %n%s", sw);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(result);
        }
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


    private class FaceTask extends AsyncTask<Void, Void, String> {
        private ManagedChannel mChannel;
        private String hostIP;
        private int hostPort;

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                // first version use static IP and port
                hostIP = "172.28.142.176";
                hostPort = 50052;
                mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                        .usePlaintext(true)
                        .build();
                FacerecognitionGrpc.FacerecognitionBlockingStub stub = FacerecognitionGrpc.newBlockingStub(mChannel);
                FaceRecognitionRequest message = FaceRecognitionRequest.newBuilder().setMessage("Can I pass?").build();
                FaceRecognitionReply reply = stub.offloading(message);
                return reply.getMessage();
            } catch(Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                pw.flush();
                return String.format("Failed... : %n%s", sw);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.println(result);
        }
    }
}
