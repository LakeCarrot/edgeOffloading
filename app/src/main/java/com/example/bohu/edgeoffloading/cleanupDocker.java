package com.example.bohu.edgeoffloading;

import android.os.AsyncTask;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;

import edgeOffloading.OffloadingGrpc;
import edgeOffloading.OffloadingOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class cleanupDocker extends AsyncTask<Void, Void, String> {
    private ManagedChannel mChannel;
    private String hostIP;
    private String containerID;

    public cleanupDocker(String hostIP, String containerID) {
        this.hostIP = hostIP;
        this.containerID = containerID;
    }

    @Override
    protected String doInBackground(Void... params) {
        try {
            mChannel = ManagedChannelBuilder.forAddress(hostIP, 60052)
                    .usePlaintext(true)
                    .build();
            OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(mChannel);
            OffloadingOuterClass.OffloadingRequest messae = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage(containerID).build();
            OffloadingOuterClass.OffloadingReply reply = stub.startService(messae);

            Log.e("Rui", "cleanupDocker receive reply" + reply.getMessage());
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

}
