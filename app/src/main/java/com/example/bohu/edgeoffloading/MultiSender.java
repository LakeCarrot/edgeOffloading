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

    public MultiSender (String ip, int port) {
        this.nearestIP = ip;
        this.appPort = port;
    }

    @Override
    public void run() {
        new SchedulerTask(nearestIP, appPort).execute();
        //new RemoteSpeechRecognition(destination, appPort);
    }

    private class SchedulerTask extends AsyncTask<Void, Void, String> {
        private ManagedChannel mChannel;
        private String hostIP;
        private int appPort;

        public SchedulerTask(String ip, int port) {
            this.hostIP = ip;
            this.appPort = port;
        }

        @Override
        protected String doInBackground(Void... nothing) {
            try {
                // first version use static IP and port
                int schedulerPort = 50051;
                mChannel = ManagedChannelBuilder.forAddress(hostIP, schedulerPort)
                        .usePlaintext(true)
                        .build();
                OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(mChannel);
                OffloadingOuterClass.OffloadingRequest message = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage("first:ruili92/speech").build();
                OffloadingOuterClass.OffloadingReply reply = stub.startService(message);
                destination = reply.getMessage();
                return destination;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            Log.e("Rui", "destination: " + destination);
            // prepare port and docker image on destination
            new PrepareDocker(destination, appPort, "ruili92/speech").execute();
            Log.e("Rui", "*******************************************************");
            new RemoteSpeechRecognition(destination, appPort);
        }
    }


}
