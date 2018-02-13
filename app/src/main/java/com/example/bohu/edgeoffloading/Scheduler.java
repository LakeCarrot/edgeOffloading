package com.example.bohu.edgeoffloading;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Scheduler extends AsyncTask<Void, Void, String> {
    protected String doInBackground(Void... nothing) {
        try {
            Log.e("Rui","start to cr");
            Socket socket = new Socket("172.28.142.176",50051);
            OutputStream out;
            InputStream input;
            File root = Environment.getExternalStorageDirectory();
            File file = new File(root, "/testFace/1.jpg");
            int size = (int) file.length();
            byte[] bytes = new byte[size];
            input = new BufferedInputStream(new FileInputStream(file));
            input.read(bytes,0,size);
            out = socket.getOutputStream();
            out.write(bytes,0,size);
            out.flush();
            Log.e("Rui","Succeed!@");
        } catch (Exception e) {
            Log.e("Rui","exception: " + e);
        }

        return null;
    }

}
