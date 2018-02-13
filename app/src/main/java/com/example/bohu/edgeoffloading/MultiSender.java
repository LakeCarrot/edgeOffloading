package com.example.bohu.edgeoffloading;

import android.util.Log;

public class MultiSender implements Runnable {
    int port;

    public MultiSender (int port) {
        Log.e("Rui","arrive at MultiSender");
        this.port = port;
    }

    @Override
    public void run() {
        new RemoteSpeechRecognition("172.28.142.176", port);
    }
}
