package com.example.bohu.edgeoffloading;

public class DirectOCR implements Runnable {
    int appPort;
    String nearestIP;

    public DirectOCR(String ip, int port) {
        this.nearestIP = ip;
        this.appPort = port;
    }

    @Override
    public void run(){
        new RemoteOCR(nearestIP, appPort);
    }
}
