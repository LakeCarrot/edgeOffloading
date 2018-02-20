package com.example.bohu.edgeoffloading;

import android.os.AsyncTask;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import edgeOffloading.OffloadingGrpc;


import edu.cmu.pocketsphinx.SpeechRecognizer;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import speechRecognition.SpeechrecognitionGrpc;
import speechRecognition.SpeechrecognitionOuterClass;

public class RemoteSpeechRecognition {//} extends AsyncTask<Void, Void, String> {
    private ManagedChannel mChannel;
    private String hostIP;
    private int hostPort;

    public RemoteSpeechRecognition(String hostIP, int hostPort) {
        this.hostIP = hostIP;
        this.hostPort = hostPort;
        try {
            remoteCall();
        } catch (Exception e) {
            new Exception().printStackTrace();
        }
    }

    private void remoteCall() throws Exception {
        Log.e("Rui", "connect to " + hostIP + " at " + hostPort);
        Thread.sleep(10000);
        mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                .usePlaintext(true)
                .build();
        SpeechrecognitionGrpc.SpeechrecognitionBlockingStub stub = SpeechrecognitionGrpc.newBlockingStub(mChannel);
        SpeechrecognitionOuterClass.SpeechRecognitionRequest message = SpeechrecognitionOuterClass.SpeechRecognitionRequest.newBuilder().setMessage(hostIP).build();
        Log.e("Rui", "start time: " + System.currentTimeMillis());
        SpeechrecognitionOuterClass.SpeechRecognitionReply reply = stub.offloading(message);

        Log.e("Rui", "stop time: " + System.currentTimeMillis());
        new cleanupDocker(hostIP, "speech" + Integer.toString(hostPort)).execute();
        Log.e("Rui", "done");
    }

    /*
    @Override
    protected String doInBackground(Void... params) {
        Log.e("Rui","initialize doInBackground");
        try {
            Log.e("Rui","come here");
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            SpeechrecognitionGrpc.SpeechrecognitionBlockingStub stub = SpeechrecognitionGrpc.newBlockingStub(mChannel);
            SpeechrecognitionOuterClass.SpeechRecognitionRequest message = SpeechrecognitionOuterClass.SpeechRecognitionRequest.newBuilder().setMessage("ruili92/speech").build();
            Log.e("Rui", "send message");
            SpeechrecognitionOuterClass.SpeechRecognitionReply reply = stub.offloading(message);

            Log.e("Rui", "receive reply" + reply.getMessage());
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
    }
    */
}
