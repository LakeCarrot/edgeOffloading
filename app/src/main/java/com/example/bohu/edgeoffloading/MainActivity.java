package com.example.bohu.edgeoffloading;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;

import org.openalpr.OpenALPR;
import org.openalpr.model.Results;
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

    private String openAlprConfFile;
    private String ANDROID_DATA_DIR;

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

        // do initialization here for each application
        // TODO: face recognition, need to load model here

        // plate recognition, load conf file
        ANDROID_DATA_DIR = this.getApplicationInfo().dataDir;
        openAlprConfFile = ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf";

        //faceRecognition();
        //new GrpcTask().execute();
        //requestSending("172.28.143.136", 50051);
        //new FaceTask().execute();
        plateRecognition();

    }

    private void requestSending(String hostIP, int hostPort) {
        ManagedChannel managedChannel;
        managedChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                .usePlaintext(true)
                .build();
        try {
            // first version use static IP and port
            OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(managedChannel);
            OffloadingOuterClass.OffloadingRequest message = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage("Who are you?").build();
            OffloadingOuterClass.OffloadingReply reply = stub.startService(message);
            Log.d("requestSending", reply.getMessage());
        } catch(Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            pw.flush();
        }
        managedChannel.shutdown();
    }

    private class GrpcTask extends AsyncTask<Void, Void, String> {
        private ManagedChannel mChannel;
        private String hostIP;
        private int hostPort;

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                // first version use static IP and port
                hostIP = "172.28.143.136";
                hostPort = 50051;
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
        /*
        code on IoT part when load data
        */
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, "/testFace/"+ 1 + ".jpg");
        Mat testImage = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        if(testImage.empty()) {
            Log.d("image", "wrong!!!");
        }
        //Mat testImage = Imgcodecs.imread("testDir", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        File trainImages = new File(root, trainingDir);
        FilenameFilter imgFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            }
        };
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

        FaceRecognizer face = LBPHFaceRecognizer.create();
        //ByteBuffer bb = ByteBuffer.allocate(labelsBuf.length * 4);
        //bb.asIntBuffer().put(labelsBuf);

        //Mat labels = new Mat(imageFiles.length, 1, CV_32SC1, bb);
        face.train(images, new MatOfInt(labelsBuf));

        int[] label = new int[1];
        double[] confidence = new double[1];
        face.predict(testImage, label, confidence);
        int predictedLabel = label[0];

        Log.d("face recognition", "Predicted label: " + predictedLabel);
    }

    private void plateRecognition() {
        File root = Environment.getExternalStorageDirectory();
        Log.d("OPEN ALPR", root.getAbsolutePath());
        File file = new File(root, "/plate_data/3.jpg");
        OpenALPR openalpr = OpenALPR.Factory.create(MainActivity.this, ANDROID_DATA_DIR);
        long begin = System.currentTimeMillis();
        String result = openalpr.recognizeWithCountryRegionNConfig("us", "", file.getAbsolutePath(), openAlprConfFile, 10);
        long end = System.currentTimeMillis();
        Log.d("Measured", "" + (end - begin)/1000.0);
        final Results results = new Gson().fromJson(result, Results.class);
        Log.d("OPEN ALPR", "Processing time: " + String.format("%.2f", ((results.getProcessingTimeMs() / 1000.0) % 60)) + " seconds");
        Log.d("OPEN ALPR", result);
    }

    private class FaceTask extends AsyncTask<Void, Void, String> {
        private ManagedChannel mChannel;
        private String hostIP;
        private int hostPort;

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                // first version use static IP and port
                hostIP = "172.28.143.136";
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
