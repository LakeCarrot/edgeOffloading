package com.example.bohu.edgeoffloading;

import android.util.Log;

import java.util.concurrent.TimeUnit;

import edgeOffloading.OffloadingGrpc;
import edgeOffloading.OffloadingOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import speechRecognition.SpeechrecognitionGrpc;
import speechRecognition.SpeechrecognitionOuterClass;

public class RemoteOCR {//} extends AsyncTask<Void, Void, String> {
    private ManagedChannel mChannel;
    private String hostIP;
    private int hostPort;

    public RemoteOCR(String hostIP, int hostPort) {
        this.hostIP = hostIP;
        this.hostPort = hostPort;
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
        OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(mChannel);
        OffloadingOuterClass.OffloadingRequest message = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage(hostIP).build();
        OffloadingOuterClass.OffloadingReply reply = stub.startService(message);
        Log.e("Rui", "reply: " + reply.getMessage());
        mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);

        Log.e("Rui", "stop time: " + System.currentTimeMillis());
        Log.e("Rui", "done");
    }
}
