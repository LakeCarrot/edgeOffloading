package com.example.bohu.edgeoffloading;

import android.os.AsyncTask;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import edgeOffloading.OffloadingGrpc;
import edgeOffloading.OffloadingOuterClass;
import edgeOffloading.OffloadingOuterClass.OffloadingRequest;
import edgeOffloading.OffloadingOuterClass.OffloadingReply;

public class PrepareDocker extends AsyncTask<Void, Void, String> {
    private ManagedChannel mChannel;
    private String hostIP;
    private int appPort;
    private String appId;

    public PrepareDocker(String hostIP, int appPort, String appId) {
        this.hostIP = hostIP;
        this.appPort = appPort;
        this.appId = appId;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            mChannel = ManagedChannelBuilder.forAddress(hostIP, 60051)
                    .usePlaintext(true)
                    .build();
            OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(mChannel);
            Log.e("Rui","will connect app port: " + appPort);
            String message = Integer.toString(appPort) + ":" + appId;
            OffloadingOuterClass.OffloadingRequest messae = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage(message).build();
            OffloadingOuterClass.OffloadingReply reply = stub.startService(messae);

            Log.e("Rui", "PrepareDocker receive reply" + reply.getMessage());
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
        Log.e("Rui","[PrepareDocker] hostIP: " + hostIP + ", appPort: " + appPort);
        new RemoteSpeechRecognition(hostIP, appPort);//.execute();
    }
}
