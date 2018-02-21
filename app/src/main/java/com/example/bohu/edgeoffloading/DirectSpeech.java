package com.example.bohu.edgeoffloading;

import io.grpc.ManagedChannel;

public class DirectSpeech implements Runnable {
    int appPort;
    String nearestIP;

    public DirectSpeech(String ip, int port) {
        this.nearestIP = ip;
        this.appPort = port;
    }

    @Override
    public void run() {
        new RemoteSpeechRecognition(nearestIP, appPort);
    }
}
