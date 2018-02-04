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

public class RemoteSpeechRecognition extends AsyncTask<Void, Void, String> {
    private ManagedChannel mChannel;
    private String hostIP;
    private int hostPort;
    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    private static final String KEYPHRASE = "oh mighty computer";
    private static final String KWS_SEARCH = "wakeup";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String DIGITS_SEARCH = "digits";
    private static final String PHONE_SEARCH = "phones";
    private static final String MENU_SEARCH = "menu";

    @Override
    protected String doInBackground(Void... params) {
        try {
            // first version use static IP and port
            hostIP = "172.28.142.176";
            hostPort = 50052;
            Log.e("Rui", "try to connect");
            mChannel = ManagedChannelBuilder.forAddress(hostIP, hostPort)
                    .usePlaintext(true)
                    .build();
            /*
            OffloadingGrpc.OffloadingBlockingStub stub = OffloadingGrpc.newBlockingStub(mChannel);
            OffloadingOuterClass.OffloadingRequest messae = OffloadingOuterClass.OffloadingRequest.newBuilder().setMessage("Can I pass?").build();
            OffloadingOuterClass.OffloadingReply reply = stub.startService(messae);
            */

            SpeechrecognitionGrpc.SpeechrecognitionBlockingStub stub = SpeechrecognitionGrpc.newBlockingStub(mChannel);
            SpeechrecognitionOuterClass.SpeechRecognitionRequest message = SpeechrecognitionOuterClass.SpeechRecognitionRequest.newBuilder().setMessage("Can I pass?").build();
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
}
