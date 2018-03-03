package com.example.bohu.edgeoffloading;

import android.os.AsyncTask;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Random;
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
        Random rand = new Random();
        try {
            remoteCall();
        } catch (Exception e) {
            new Exception().printStackTrace();
        }
    }

    public String hostTranslation(String host) {
        String hostName = "unknown";
        if (host.equals("172.28.142.176"))
            hostName = "slave1";
        else if (host.equals("172.28.140.65"))
            hostName = "slave2";
        else if (host.equals("172.28.142.226"))
            hostName = "slave3";
        else if (host.equals("172.28.136.3"))
            hostName = "master";

        return hostName;
    }

    private void remoteCall() throws Exception {
        Log.e("Rui", "connect to " + hostTranslation(hostIP) + " at " + hostPort);
        mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                .usePlaintext(true)
                .build();
        SpeechrecognitionGrpc.SpeechrecognitionBlockingStub stub = SpeechrecognitionGrpc.newBlockingStub(mChannel);
        SpeechrecognitionOuterClass.SpeechRecognitionRequest message = SpeechrecognitionOuterClass.SpeechRecognitionRequest.newBuilder().setMessage(hostIP).build();
        while (true) {
            SpeechrecognitionOuterClass.SpeechRecognitionReply reply = stub.offloading(message);
            Log.e("Rui", "stopped unintentionally. Launch again!!");
        }
        //new cleanupDocker(hostIP, "speech" + Integer.toString(hostPort)).execute();
        //Log.e("Rui", "done");
    }
}
