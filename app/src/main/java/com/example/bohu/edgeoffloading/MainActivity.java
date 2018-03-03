package com.example.bohu.edgeoffloading;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;

import org.openalpr.OpenALPR;
import org.openalpr.model.Results;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.face.FaceRecognizer;
import org.opencv.face.LBPHFaceRecognizer;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import edgeOffloading.OffloadingGrpc;
import edgeOffloading.OffloadingOuterClass;
import faceRecognition.FacerecognitionGrpc;
import faceRecognition.FacerecognitionOuterClass.FaceRecognitionRequest;
import faceRecognition.FacerecognitionOuterClass.FaceRecognitionReply;
import fileuploadtest.FileUploadTestGrpc;
import fileuploadtest.Fileuploadtest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import platerecognition.PlateRecognitionGrpc;
import platerecognition.Platerecognition;
import speechRecognition.SpeechrecognitionGrpc;
import speechRecognition.SpeechrecognitionOuterClass;

import static android.R.attr.button;
import static org.opencv.core.CvType.CV_32SC1;

// face recognition related lib



public class MainActivity extends AppCompatActivity {
    static{ System.loadLibrary("opencv_java3"); }

    private String openAlprConfFile;
    private String ANDROID_DATA_DIR;
    private FaceRecognizer face;

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
        /*
        Log.d("Face", "Begin loading!");
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, "/testFace/faceModel.yml");
        face = LBPHFaceRecognizer.create();
        face.read(file.getAbsolutePath());
        Log.d("Face", "Finish loading!");
        */

        // plate recognition, load conf file
        ANDROID_DATA_DIR = this.getApplicationInfo().dataDir;
        openAlprConfFile = ANDROID_DATA_DIR + File.separatorChar + "runtime_data" + File.separatorChar + "openalpr.conf";

        //faceRecognition();
        //new GrpcTask().execute();
        //requestSending("172.28.143.136", 50051);
        //new FaceTask().execute();
        //plateRecognition();
        //new PlateTask().execute();
        //new FileUploadClient().execute();
        final Button faceBack = (Button) findViewById(R.id.faceBack);
        final Button plateBack = (Button) findViewById(R.id.plateBack);
        final Button speechBack = (Button) findViewById(R.id.speechBack);
        final Button ocrBack = (Button) findViewById(R.id.ocrBack);
        final Button newPlate = (Button) findViewById(R.id.newPlate);
        final Button newFace = (Button) findViewById(R.id.newFace);
        final Button newSpeech = (Button) findViewById(R.id.newSpeech);
        final Button newOcr = (Button) findViewById(R.id.newOcr);
        final String masterIP = "172.28.136.3";
        final String slave1IP = "172.28.142.176";
        final String slave2IP = "172.28.140.65";
        final String slave3IP = "172.28.142.226";
        final int requestLength = 100000000;
        final int speechPort = 50052;
        final int facePort = 50053;
        final int platePort = 50054;
        final int ocrPort = 50055;

        faceBack.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            @Override
            public void onClick(View v) {
                executor.execute(new FaceTask(masterIP, facePort, requestLength));
                executor.execute(new FaceTask(slave1IP, facePort, requestLength));
                executor.execute(new FaceTask(slave2IP, facePort, requestLength));
                executor.execute(new FaceTask(slave3IP, facePort, requestLength));
            }
        });

        plateBack.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            @Override
            public void onClick(View v) {
                executor.execute(new PlateTask(masterIP, platePort, 1));
                executor.execute(new PlateTask(slave1IP, platePort, 1));
                executor.execute(new PlateTask(slave2IP, platePort, 1));
                executor.execute(new PlateTask(slave3IP, platePort, 1));
                //executor.execute(new PlateTask(masterIP, 50054, 1));
            }
        });

        ocrBack.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            @Override
            public void onClick(View v) {
                executor.execute(new OcrTask(masterIP, ocrPort));
                executor.execute(new OcrTask(slave1IP, ocrPort));
                executor.execute(new OcrTask(slave2IP, ocrPort));
                executor.execute(new OcrTask(slave3IP, ocrPort));
            }
        });

        speechBack.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            @Override
            public void onClick(View v) {
                executor.execute(new SpeechTask(masterIP, speechPort));
                executor.execute(new SpeechTask(slave1IP, speechPort));
                executor.execute(new SpeechTask(slave2IP, speechPort));
                executor.execute(new SpeechTask(slave3IP, speechPort));
//                try {
//                    executor.execute(new SpeechTask(masterIP, speechPort));
//                    //Thread.sleep(20000);
//                    executor.execute(new SpeechTask(slave1IP, speechPort));
//                    Thread.sleep(20000);
//                    executor.execute(new SpeechTask(slave1IP, speechPort));
//                    executor.execute(new SpeechTask(slave2IP, speechPort));
//                    Thread.sleep(20000);
//                    executor.execute(new SpeechTask(slave2IP, speechPort));
//                    Thread.sleep(20000);
//                    executor.execute(new SpeechTask(slave2IP, speechPort));
//                    //Thread.sleep(20000);
//                    executor.execute(new SpeechTask(slave3IP, speechPort));
//                    Thread.sleep(20000);
//                    executor.execute(new SpeechTask(slave3IP, speechPort));
//                    Thread.sleep(20000);
//                    executor.execute(new SpeechTask(slave3IP, speechPort));
//                    Thread.sleep(20000);
//                    executor.execute(new SpeechTask(slave3IP, speechPort));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//                try {
//                    Thread.sleep(50000);
//                    for(int i = 0; i < 6; i++) {
//                        String dstIP = connectionSetup("speech", slave1IP, 50051);
//                        Log.d("Speech Recognition", "send to " + dstIP);
//                        executor.execute(new SpeechTask(dstIP, speechPort));
//                        Thread.sleep(40000);
//                    }
//                    //executor.execute(new SpeechTask(slave1IP, speechPort));
//                } catch(Exception e) {
//                    e.printStackTrace();
//                }
            }
        });

        newPlate.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            @Override
            public void onClick(View v) {
                //String dstIP = connectionSetup("plate", slave1IP, 50051);
                //Log.d("Speech Recognition", "send to " + dstIP);
                //executor.execute(new PlateTask(dstIP, platePort, 1));
                executor.execute(new PlateTask("34.218.97.178", 50054, 1));
            }
        });

        newFace.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            @Override
            public void onClick(View v) {
//                String dstIP = connectionSetup("face", slave1IP, 50051);
//                Log.d("Speech Recognition", "send to " + dstIP);
//                executor.execute(new FaceTask(dstIP, facePort, requestLength));
                executor.execute(new FaceTask("34.218.97.178", 50053, requestLength));
            }
        });

        newSpeech.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            @Override
            public void onClick(View v) {
                String dstIP = connectionSetup("speech", slave1IP, 50051);
                Log.d("Speech Recognition", "send to " + dstIP);
                executor.execute(new SpeechTask(dstIP, speechPort));
//                executor.execute(new SpeechTask(masterIP, speechPort));
//                try {
//                    Thread.sleep(50000);
//                    for(int i = 0; i < 6; i++) {
//                        String dstIP = connectionSetup("speech", slave1IP, 50051);
//                        Log.d("Speech Recognition", "send to " + dstIP);
//                        executor.execute(new SpeechTask(dstIP, speechPort));
//                        Thread.sleep(50000);
//                    }
//                    //executor.execute(new SpeechTask(slave1IP, speechPort));
//                } catch(Exception e) {
//                    e.printStackTrace();
//                }
            }
        });

        newOcr.setOnClickListener(new View.OnClickListener() {
            ExecutorService executor = Executors.newCachedThreadPool();
            @Override
            public void onClick(View v) {
                String dstIP = connectionSetup("ocr", slave1IP, 50051);
                Log.d("OCR", "send to " + dstIP);
                executor.execute(new OcrTask(dstIP, ocrPort));
                //executor.execute(new OcrTask(slave2IP, ocrPort));
            }
        });
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
        long begin = System.currentTimeMillis();
        /*
        code on IoT part when load data
        */
        File root = Environment.getExternalStorageDirectory();
        File file = new File(root, "/testFace/"+ 1 + ".jpg");
        Mat testImage = Imgcodecs.imread(file.getAbsolutePath(), Imgcodecs.CV_LOAD_IMAGE_GRAYSCALE);
        if(testImage.empty()) {
            Log.d("image", "wrong!!!");
        }

        int[] label = new int[1];
        double[] confidence = new double[1];
        face.predict(testImage, label, confidence);
        int predictedLabel = label[0];
        long end = System.currentTimeMillis();

        Log.d("Face Recognition", "Predicted label: " + predictedLabel + " Processing time: " + (end - begin) + " ms");
    }

    private void plateRecognition() {
        File root = Environment.getExternalStorageDirectory();
        Log.d("OPEN ALPR", root.getAbsolutePath());
        File file = new File(root, "/testPlate/1.jpg");
        OpenALPR openalpr = OpenALPR.Factory.create(MainActivity.this, ANDROID_DATA_DIR);
        long begin = System.currentTimeMillis();
        String result = openalpr.recognizeWithCountryRegionNConfig("us", "", file.getAbsolutePath(), openAlprConfFile, 10);
        long end = System.currentTimeMillis();
        Log.d("Measured", "" + (end - begin));
        final Results results = new Gson().fromJson(result, Results.class);
        Log.d("OPEN ALPR", "Processing time: " + String.format("%.2f", ((results.getProcessingTimeMs() / 1000.0) % 60)) + " seconds");
        Log.d("OPEN ALPR", result);
    }

    private class FaceTask implements Runnable {
        private ManagedChannel mChannel;
        private String hostIP;
        private int hostPort;
        private FacerecognitionGrpc.FacerecognitionBlockingStub stub;
        private int length;

        public FaceTask(String hostIP, int hostPort, int length) {
            this.hostIP = hostIP;
            this.hostPort = hostPort;
            this.length = length;
        }

        private void init() {
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            stub = FacerecognitionGrpc.newBlockingStub(mChannel);
        }

        private String issueRequest(String filepath, String name) {
            File file = new File(filepath);
            if(file.exists() == false) {
                //Log.d("Face Recognition", "File that needed to be uploaded doesn't exist");
                return "";
            }
            try {
                long begin = System.currentTimeMillis();
                //Log.d("Face Recognition", "Successfully read the data");
                BufferedInputStream bInputStream = new BufferedInputStream(new FileInputStream(file));
                int bufferSize = (int) file.length(); // 64 kb per message
                byte[] buffer = new byte[bufferSize];
                int tmp = 0;
                //Log.d("Face Recognition", "Start to transfer file");
                if((tmp = bInputStream.read(buffer)) > 0) {
                    ByteString byteString = ByteString.copyFrom(buffer, 0, tmp);
                    FaceRecognitionRequest message = FaceRecognitionRequest.newBuilder().setName(name).setData(byteString).build();
                    FaceRecognitionReply reply = stub.offloading(message);
                    long end = System.currentTimeMillis();
                    Log.d("Face Recognition", "processing time " + (end - begin));
                    return reply.getMessage();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        public void run() {
            try {
                // first version use static IP and port
                init();
                File root = Environment.getExternalStorageDirectory();
                File file = new File(root, "/testFace/1.jpg");
                for(int i = 0; i < length; i++) {
                    issueRequest(file.getAbsolutePath(), "1.jpg");
                }
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }


    private class FileUploadClient extends AsyncTask<Void, Void, String> {
        private ManagedChannel mChannel;
        private FileUploadTestGrpc.FileUploadTestStub mAsyncStub;
        private String hostIP;
        private int hostPort;

        private void init() {
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            mAsyncStub = FileUploadTestGrpc.newStub(mChannel);
        }

        private void startStream(final String filepath, String filename) {
            final CountDownLatch finsihLatch = new CountDownLatch(1);
            StreamObserver<Fileuploadtest.Response> responseObserver = new StreamObserver<Fileuploadtest.Response>() {
                @Override
                public void onNext(Fileuploadtest.Response value) {

                }

                @Override
                public void onError(Throwable t) {
                    finsihLatch.countDown();
                }

                @Override
                public void onCompleted() {
                    finsihLatch.countDown();
                }
            };
            StreamObserver<Fileuploadtest.Request> requestObserver = mAsyncStub.upload(responseObserver);
            try {
                File file = new File(filepath);
                if(file.exists() == false) {
                    Log.d("File Upload Test", "File that needed to be uploaded doesn't exist");
                    return;
                }
                try {
                    Log.d("File Upload Test", "Successfully read the data");
                    BufferedInputStream bInputStream = new BufferedInputStream(new FileInputStream(file));
                    int bufferSize = 64*1024; // 64 kb per message
                    byte[] buffer = new byte[bufferSize];
                    int tmp = 0;
                    int size = 0;
                    Log.d("File Upload", "Start to transfer file");
                    while((tmp = bInputStream.read(buffer)) > 0) {
                        size += tmp;
                        ByteString byteString = ByteString.copyFrom(buffer, 0, tmp);
                        Fileuploadtest.Request req = Fileuploadtest.Request.newBuilder().setName(filename).setData(byteString).build();
                        //TODO: the offset here may need to adjust
                        requestObserver.onNext(req);
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (RuntimeException e) {
                requestObserver.onError(e);
            }
            requestObserver.onCompleted();
        }

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                Log.d("File Upload Test", "enter here!");
                hostIP = "172.28.143.136";
                hostPort = 50052;
                init();
                File root = Environment.getExternalStorageDirectory();
                File file = new File(root, "/testFace/test.jpg");
                Log.d("File Upload Test", "start streaming!");
                startStream(file.getAbsolutePath(), "test.jpg");
                return "";
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
            return;
        }
    }

    private class PlateTask implements Runnable {
        private ManagedChannel mChannel;
        private String hostIP;
        private int hostPort;
        private int length;
        PlateRecognitionGrpc.PlateRecognitionBlockingStub stub;

        public PlateTask(String hostIP, int hostPort, int length) {
            this.hostIP = hostIP;
            this.hostPort = hostPort;
            this.length = length;
        }

        private void init() {
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            stub = PlateRecognitionGrpc.newBlockingStub(mChannel);
        }

        private String issueRequest(String filepath, String name) {
            File file = new File(filepath);
            if(file.exists() == false) {
                //Log.d("File Upload Test", "File that needed to be uploaded doesn't exist");
                return "";
            }
            try {
                BufferedInputStream bInputStream = new BufferedInputStream(new FileInputStream(file));
                int bufferSize = (int) file.length(); // 64 kb per message
                byte[] buffer = new byte[bufferSize];
                int tmp = 0;
                if((tmp = bInputStream.read(buffer)) > 0) {
                    ByteString byteString = ByteString.copyFrom(buffer, 0, tmp);
                    Platerecognition.PlateRecognitionRequest message = Platerecognition.PlateRecognitionRequest.newBuilder().setName(name).setData(byteString).build();
                    Platerecognition.PlateRecognitionReply reply = stub.offloading(message);
                    return reply.getMessage();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

       public void run() {
           try {
               // first version use static IP and port
               init();
               File root = Environment.getExternalStorageDirectory();
               File file = new File(root, "/testPlate/1.jpg");
               Log.d("Plate Recognition", "send to " + hostIP);
               for (int i = 0; i < length; i++) {
                   long begin = System.currentTimeMillis();
                   issueRequest(file.getAbsolutePath(), "1.jpg");
                   long end = System.currentTimeMillis();
                   Log.d("Plate Recognition", "Processing time: " + (end - begin));
               }
               mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
           } catch (Exception e) {
               e.printStackTrace();
           }
       }
    }

    private class SpeechTask implements Runnable {
        private ManagedChannel mChannel;
        private String hostIP;
        private int hostPort;
        SpeechrecognitionGrpc.SpeechrecognitionBlockingStub stub;

        public SpeechTask(String hostIP, int hostPort) {
            this.hostIP = hostIP;
            this.hostPort = hostPort;
        }

        private void init() {
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            stub = SpeechrecognitionGrpc.newBlockingStub(mChannel);
        }

        public void run() {
            try {
                init();
                //Log.d("Speech Recognition", "send to " + hostIP);
                while(true) {
                    System.out.println("wired behavior");
                    SpeechrecognitionOuterClass.SpeechRecognitionRequest message = SpeechrecognitionOuterClass.SpeechRecognitionRequest.newBuilder().setMessage(hostIP).build();
                    SpeechrecognitionOuterClass.SpeechRecognitionReply reply = stub.offloading(message);
                }
                    //mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private class OcrTask implements Runnable {
        private ManagedChannel mChannel;
        private String hostIP;
        private int hostPort;
        OffloadingGrpc.OffloadingBlockingStub stub;

        public OcrTask(String hostIP, int hostPort) {
            this.hostIP = hostIP;
            this.hostPort = hostPort;
        }

        private void init() {
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            stub = OffloadingGrpc.newBlockingStub(mChannel);
        }

        public void run() {
            try {
                init();
                //Log.d("Speech Recognition", "send to " + hostIP);
                OffloadingOuterClass.OffloadingRequest message = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage(hostIP).build();
                OffloadingOuterClass.OffloadingReply reply = stub.startService(message);
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String connectionSetup(String appid, String hostIP, int hostPort) {
        try {
            ManagedChannel mChannel;
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(mChannel);
            OffloadingOuterClass.OffloadingRequest message = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage(appid).build();
            OffloadingOuterClass.OffloadingReply reply = stub.startService(message);
            String dstIP = reply.getMessage();
            mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            return dstIP;
        } catch(Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
