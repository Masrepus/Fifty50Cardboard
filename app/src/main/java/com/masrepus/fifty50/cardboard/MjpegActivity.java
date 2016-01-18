package com.masrepus.fifty50.cardboard;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.google.vrtoolkit.cardboard.sensors.HeadTracker;
import com.masrepus.fifty50.cardboard.MjpegInputStream;
import com.masrepus.fifty50.cardboard.MjpegView;
import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import javax.microedition.khronos.egl.EGLConfig;

public class MjpegActivity extends CardboardActivity implements CardboardView.StereoRenderer {
    private static final String TAG = "MjpegActivity";

    private MjpegView mv;
    private HeadTransform headTransform;
    private Direction direction = Direction.STRAIGHT;
    private Mode mode = Mode.BRAKE;
    private boolean running;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //sample public cam
        String URL = "http://192.168.178.31:8080/?action=stream";

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mv = new MjpegView(this);
        setContentView(mv);

        running = true;

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

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        this.headTransform = headTransform;
    }

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int i, int i1) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

    }

    @Override
    public void onRendererShutdown() {

    }

    public Direction getDirection() {
        return direction;
    }

    public Mode getMode() {
        return mode;
    }

    public class DoRead extends AsyncTask<String, Void, MjpegInputStream> {
        protected MjpegInputStream doInBackground(String... url) {
            HttpResponse res = null;
            DefaultHttpClient httpclient = new DefaultHttpClient();
            Log.d(TAG, "1. Sending http request");
            try {
                res = httpclient.execute(new HttpGet(new URI("http", null, "192.168.178.31", 8080, "/", "action=stream", "anchor")));
                Log.d(TAG, "2. Request finished, status = " + res.getStatusLine().getStatusCode());
                if(res.getStatusLine().getStatusCode()==401){
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

            while (running) {

                //check the current head transform and send the appropriate command
                if (headTransform != null) {

                    float[] eulerAngles = new float[3];
                    headTransform.getEulerAngles(eulerAngles, 0);

                    //if roll is right, send "right" command, etc.
                    if (eulerAngles[2] > 0.17) direction = Direction.RIGHT;
                    else if (eulerAngles[2] < -0.17) direction = Direction.LEFT;
                    else direction = Direction.STRAIGHT;

                    //check pitch angle
                    if (eulerAngles[0] > 0.17) mode = Mode.BACKWARD;
                    else if (eulerAngles[0] < -0.17) mode = Mode.FORWARD;
                    else mode = Mode.BRAKE;

                    //now send command
                    switch (direction) {
                        case LEFT: left();
                            break;
                        case RIGHT: right();
                            break;
                        case STRAIGHT: straight();
                            break;
                    }

                    switch (mode) {
                        case FORWARD: forward();
                            break;
                        case BACKWARD: backward();
                            break;
                        case BRAKE: brake();
                            break;
                    }
                }
            }
        }
    }

    private void left() {

    }

    private void right() {

    }

    private void straight() {

    }

    private void forward() {

    }

    private void backward() {

    }

    private void brake() {

    }
}