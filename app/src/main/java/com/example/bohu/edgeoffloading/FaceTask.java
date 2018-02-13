package com.example.bohu.edgeoffloading;

import android.os.AsyncTask;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import faceRecognition.FacerecognitionGrpc;
import faceRecognition.FacerecognitionOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class FaceTask extends AsyncTask<Void, Void, String> {
    private ManagedChannel mChannel;
    private String hostIP;
    private int hostPort;

    public FaceTask(String hostIP, int hostPort) {
        this.hostIP = hostIP;
        this.hostPort = hostPort;
    }

    @Override
    protected String doInBackground(Void... nothing) {
        try {
            Thread.sleep(5000);
            Log.e("Rui","FaceTask listens to " + hostIP + " at " + hostPort);
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            Log.e("Rui", "start to send FaceTask request");

            FacerecognitionGrpc.FacerecognitionBlockingStub stub = FacerecognitionGrpc.newBlockingStub(mChannel);
            FacerecognitionOuterClass.FaceRecognitionRequest message = FacerecognitionOuterClass.FaceRecognitionRequest.newBuilder().setMessage("ruili92/speech").build();
            FacerecognitionOuterClass.FaceRecognitionReply reply = stub.offloading(message);
            Log.e("Rui", "FaceTask reply: " + reply.getMessage());
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