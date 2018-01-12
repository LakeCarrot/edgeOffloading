package com.example.bohu.edgeoffloading;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.opencv.core.Mat;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.File;
import java.io.FilenameFilter;
import java.io.PrintWriter;
import java.io.StringWriter;
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
    static{ System.loadLibrary("opencv_java"); }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        new GrpcTask().execute();
        new FaceTask().execute();

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
        String trainingDir = "trainDir";
        Mat testImage = Imgcodecs.imread("testDir", Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

        File root = new File(trainingDir);

        FilenameFilter imgFilter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                name = name.toLowerCase();
                return name.endsWith(".jpg") || name.endsWith(".pgm") || name.endsWith(".png");
            }
        };

        File[] imageFiles = root.listFiles(imgFilter);

        List<Mat> images = new ArrayList<>(imageFiles.length);

        Mat labels = new Mat(imageFiles.length, 1, CV_32SC1);
        IntBuffer labelsBuf = labels.


        int counter = 0;

        for(File image : imageFiles) {
            Mat img = Imgcodecs.imread(image.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);

            int label = Integer.parseInt(image.getName().split("\\-")[0]);

            images.set(counter, img);

            labelsBuf.put(counter, label);

            counter++;
        }

        FaceRecognizer face = LBPHFaceRecognizer.create();
        face.train(images, labels);

        int[] label = new int[1];
        double[] confidence = new double[1];
        face.predict(testImage, label, confidence);
        int predictedLabel = label[0];

        System.out.println("Predicted label: " + predictedLabel);
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
