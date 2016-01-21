package com.masrepus.fifty50.cardboard;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.vrtoolkit.cardboard.CardboardActivity;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;

public class MainActivity extends CardboardActivity {
    private static final String TAG = "MjpegActivity";

    private MjpegView mv;
    private boolean running;
    private String server;
    private int port;
    private volatile Car.PinState fwdFast, fwdSlow, bwdFast, bwdSlow, leftFast, leftSlow, rightFast, rightSlow;
    private DataOutputStream out;
    private DataInputStream in;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //car cam
        String URL = "http://192.168.178.31:8080/?action=stream";

        SharedPreferences pref = getSharedPreferences("prefs", 0);
        server = pref.getString("server", "192.168.42.1");
        port = pref.getInt("port", 50000);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mv = new MjpegView(this);
        setContentView(mv);

        running = true;

        fwdFast = fwdSlow = bwdFast = bwdSlow = leftFast = leftSlow = rightFast = rightSlow = Car.PinState.LOW;

        //start gesture detection, mjpg streaming and network connection services
        new GestureDetector(this).start();
        new DoRead().execute(URL);
        new Connector(server, port).start();
        new ServerListener().start();
    }

    public void onPause() {
        super.onPause();
        mv.stopPlayback();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            mv.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public void straight() {
        if (leftFast == Car.PinState.LOW && rightFast == Car.PinState.LOW && leftSlow == Car.PinState.LOW && rightSlow == Car.PinState.LOW) return;
        try {
            out.writeUTF("straight");
            leftFast = rightFast = leftSlow = rightSlow = Car.PinState.LOW;
            Log.i(TAG, "gerade");
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Senden des Befehls");
        }
    }

    public void brake() {
        if (fwdFast == Car.PinState.LOW && fwdSlow == Car.PinState.LOW && bwdSlow == Car.PinState.LOW && bwdFast == Car.PinState.LOW) return;
        try {
            out.writeUTF("");
            fwdSlow = fwdFast = bwdSlow = bwdFast = Car.PinState.LOW;
            Log.i(TAG, "bremsen");
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Senden des Befehls");
        }
    }

    public void right(Car.Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (rightFast == Car.PinState.HIGH) return;
                rightFast = Car.PinState.HIGH;
                rightSlow = leftFast = leftSlow = Car.PinState.LOW;
                command = "L";
                break;
            case SLOW:
                if (rightSlow == Car.PinState.HIGH) return;
                rightSlow = Car.PinState.HIGH;
                rightFast = leftFast = leftSlow = Car.PinState.LOW;
                command = "D";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
            Log.i(TAG, command);
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Senden des Befehls " + command);
        }
    }

    public void left(Car.Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (leftFast == Car.PinState.HIGH) return;
                leftFast = Car.PinState.HIGH;
                leftSlow = rightFast = rightSlow = Car.PinState.LOW;
                command = "J";
                break;
            case SLOW:
                if (leftSlow == Car.PinState.HIGH) return;
                leftSlow = Car.PinState.HIGH;
                leftFast = rightFast = rightSlow = Car.PinState.LOW;
                command = "A";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
            Log.i(TAG, command);
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Senden des Befehls " + command);
        }
    }

    public void backward(Car.Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (bwdFast == Car.PinState.HIGH) return;
                bwdFast = Car.PinState.HIGH;
                bwdSlow = fwdSlow = fwdFast = Car.PinState.LOW;
                command = "K";
                break;
            case SLOW:
                if (bwdSlow == Car.PinState.HIGH) return;
                bwdSlow = Car.PinState.HIGH;
                bwdFast = fwdSlow = fwdFast = Car.PinState.LOW;
                command = "S";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
            Log.i(TAG, command);
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Senden des Befehls " + command);
        }
    }

    public void forward(Car.Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (fwdFast == Car.PinState.HIGH) return;
                fwdFast = Car.PinState.HIGH;
                fwdSlow = bwdFast = bwdSlow = Car.PinState.LOW;
                command = "I";
                break;
            case SLOW:
                if (fwdSlow == Car.PinState.HIGH) return;
                fwdSlow = Car.PinState.HIGH;
                fwdFast = bwdSlow = bwdFast = Car.PinState.LOW;
                command = "W";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
            Log.i(TAG, command);
        } catch (IOException e) {
            Log.e(TAG, "Fehler beim Senden des Befehls " + command);
        }
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            Log.i(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(new URI("http", null, MainActivity.this.server, 8080, "/", "action=stream", "anchor")));
                Log.i(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.e(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            } catch (URISyntaxException e) {
                e.printStackTrace();
                Log.e(TAG, "Request failed-UriSyntaxException", e);
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(true);
        }
    }

    private class Connector extends Thread {

        private String serverName;
        private int port;
        private boolean connected;
        private Socket client;

        public Connector(String serverName, int port) {
            this.serverName = serverName;
            this.port = port;
        }

        @Override
        public void run() {

            //try to establish a two-way connection to the car; sleep and retry if connection fails
            connected = false;

            while (!connected) {
                try {
                    Log.i(TAG, "Verbinde mit " + serverName + " am Port " + port + "...");
                    client = new Socket(serverName, port);
                    Log.i(TAG, "Verbunden mit " + client.getRemoteSocketAddress());

                    OutputStream outToServer = client.getOutputStream();
                    out = new DataOutputStream(outToServer);

                    InputStream inFromServer = client.getInputStream();
                    in = new DataInputStream(inFromServer);
                    connected = true;

                    Log.i(TAG, "Verbunden mit Auto: " + client.getRemoteSocketAddress());
                } catch (IOException e) {
                    Log.e(TAG, "Verbindung fehlgeschlagen, warte...");
                    try {
                        sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    connected = false;
                }
            }
        }

        public boolean isConnected() {
            return connected;
        }

        public Socket getClient() {
            return client;
        }

    }

    private class ServerListener extends Thread {

        @Override
        public void run() {

            //listen for incoming messages from the car
            String message;
            while(true) {

                try {
                    message = in.readUTF();

                    //listen for pin state changes
                    if (message.contains("fwd fast")) fwdFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("fwd slow")) fwdSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("bwd fast")) bwdFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("bwd slow")) bwdSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("left fast")) leftFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("left slow")) leftSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("right fast")) rightFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("right slow")) rightSlow = Car.PinState.parse(message.split("\\s+")[2]);

                    Log.i(TAG, "Nachricht vom Auto: " + message);

                } catch (Exception ignored) {}
            }
        }
    }
}