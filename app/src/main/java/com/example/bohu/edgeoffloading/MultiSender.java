package com.example.bohu.edgeoffloading;

import android.os.AsyncTask;
import android.util.Log;

import java.util.concurrent.TimeUnit;

import edgeOffloading.OffloadingGrpc;
import edgeOffloading.OffloadingOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class MultiSender implements Runnable {
    int appPort;
    String nearestIP;
    String destination;
    private ManagedChannel mChannel;

    public MultiSender(String ip, int port) {
        this.nearestIP = ip;
        this.appPort = port;
    }

    @Override
    public void run() {
        try {
            int schedulerPort = 50051;
            mChannel = ManagedChannelBuilder.forAddress(nearestIP, schedulerPort)
                    .usePlaintext(true)
                    .build();
            OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(mChannel);
            OffloadingOuterClass.OffloadingRequest message = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage("speech").build();
            Log.e("Rui", "start scheduling at " + System.currentTimeMillis());
            OffloadingOuterClass.OffloadingReply reply = stub.startService(message);
            Log.e("Rui", "stop scheduling at " + System.currentTimeMillis());
            destination = reply.getMessage();
            mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);

        } catch (Exception e) {
            new Exception().printStackTrace();
        }
        //new PrepareDocker(destination, appPort, "ruili92/speech").run();
        //new RemoteSpeechRecognition(destination, appPort);
    }
}
