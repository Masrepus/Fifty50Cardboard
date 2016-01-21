package com.masrepus.fifty50.cardboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.sensors.HeadTracker;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;

public class MjpegActivity extends CardboardActivity {
    private static final String TAG = "MjpegActivity";

    private MjpegView mv;
    private Direction direction = Direction.STRAIGHT;
    private Mode mode = Mode.BRAKE;
    private boolean running;
    private String url;
    private HeadTracker tracker;
    private ArrayList<String> trackerData = new ArrayList<>();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //car cam
        String URL = "http://192.168.178.31:8080/?action=stream";

        SharedPreferences pref = getSharedPreferences("prefs", 0);
        url = pref.getString("url", "192.168.42.1");

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mv = new MjpegView(this);
        setContentView(mv);

        running = true;

        tracker = HeadTracker.createFromContext(this);
        tracker.startTracking();

        new GestureDetector().start();
        new DoRead().execute(URL);
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

    public Direction getDirection() {
        return direction;
    }

    public Mode getMode() {
        return mode;
    }

    private void left() {

    }

    private void right() {

    }

    private void testSensor() {

        int count = 0;

        while (count <= 100) {
            Log.d(TAG, String.valueOf(count));
            float[] headview = new float[16];
            tracker.getLastHeadView(headview, 0);
            trackerData.add(Arrays.toString(headview).replace("[", "").replace("]", ""));
            count++;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        saveFile();
    }

    private void saveFile() {
        String fnm = "sensor-oben-unten-5.csv";
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(fnm, Context.MODE_PRIVATE);

            for (int i = 0; i < trackerData.size(); i++) {
                outputStream.write((trackerData.get(i) + "\n").getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "finished");
    }

    private void straight() {

    }

    private void forward() {

    }

    private void backward() {

    }

    private void brake() {

    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(new URI("http", null, MjpegActivity.this.url, 8080, "/", "action=stream", "anchor")));
                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if (res.getStatusLine().getStatusCode() == 401) {
                    //You must turn off camera User Access Control before this will work
                    return null;
                }
                return new MjpegInputStream(res.getEntity().getContent());
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-ClientProtocolException", e);
                //Error connecting to camera
            } catch (IOException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-IOException", e);
                //Error connecting to camera
            } catch (URISyntaxException e) {
                e.printStackTrace();
                Log.d(TAG, "Request failed-UriSyntaxException", e);
            }

            return null;
        }

        protected void onPostExecute(MjpegInputStream result) {
            mv.setSource(result);
            mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
            mv.showFps(true);
        }
    }

    public class GestureDetector extends Thread {

        @Override
        public void run() {

            //check the current head transform and send the appropriate command
            if (tracker != null) {

                float[] headView = new float[3];
                tracker.getLastHeadView(headView, 0);

                //if looking right/left, send right/left command, etc.
                if (headView[8] > 0.2) direction = Direction.RIGHT;
                else if (headView[8] < -0.2) direction = Direction.LEFT;
                else direction = Direction.STRAIGHT;

                //looking up or down?
                if (headView[9] < -0.2) mode = Mode.BACKWARD;
                else if (headView[9] > 0.2) mode = Mode.FORWARD;
                else mode = Mode.BRAKE;

                //now send command
                switch (direction) {
                    case LEFT:
                        left();
                        break;
                    case RIGHT:
                        right();
                        break;
                    case STRAIGHT:
                        straight();
                        break;
                }

                Log.d(TAG, direction.name());

                switch (mode) {
                    case FORWARD:
                        forward();
                        break;
                    case BACKWARD:
                        backward();
                        break;
                    case BRAKE:
                        brake();
                        break;
                }

                Log.d(TAG, mode.name());
            } else Log.d(TAG, "tracker == null");

            try {
                Thread.sleep(100, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}