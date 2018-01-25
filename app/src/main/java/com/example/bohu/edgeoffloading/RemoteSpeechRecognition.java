package com.example.bohu.edgeoffloading;

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v7.widget.VectorEnabledTintResources;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import speechRecognition.SpeechrecognitionGrpc;
import speechRecognition.SpeechrecognitionOuterClass;

import static android.widget.Toast.makeText;

public class RemoteSpeechRecognition extends AsyncTask<Void, Void, String> implements
        RecognitionListener {
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

    WeakReference<MainActivity> activityReference;
    RemoteSpeechRecognition(MainActivity activity) {
        this.activityReference = new WeakReference<>(activity);
    }

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

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if (hypothesis == null)
            return;

        String text = hypothesis.getHypstr();
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if (text.equals(DIGITS_SEARCH))
            switchSearch(DIGITS_SEARCH);
        else if (text.equals(PHONE_SEARCH))
            switchSearch(PHONE_SEARCH);
        else if (text.equals(FORECAST_SEARCH))
            switchSearch(FORECAST_SEARCH);
        else
            ((TextView) activityReference.get().findViewById(R.id.result_text)).setText(text);
    }

    @Override
    public void onEndOfSpeech() {
        if (!recognizer.getSearchName().equals(KWS_SEARCH))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        recognizer.stop();

        // If we are not spotting, start listening with timeout (10000 ms or 10 seconds).
        if (searchName.equals(KWS_SEARCH))
            recognizer.startListening(searchName);
        else
            recognizer.startListening(searchName, 10000);



        String caption = activityReference.get().getResources().getString(captions.get(searchName));
        ((TextView) activityReference.get().findViewById(R.id.caption_text)).setText(caption);
    }

    @Override
    public void onError(Exception error) {
        ((TextView) activityReference.get().findViewById(R.id.caption_text)).setText(error.getMessage());
    }

    @Override
    public void onTimeout() {
        switchSearch(KWS_SEARCH);
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) activityReference.get().findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();
            makeText(activityReference.get().getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }
}




