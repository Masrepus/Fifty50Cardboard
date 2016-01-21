package com.masrepus.fifty50.cardboard;

import android.content.Context;
import android.util.Log;

import com.google.vrtoolkit.cardboard.sensors.HeadTracker;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by samuel on 21.01.16.
 */
public class GestureDetector extends Thread {

    private static final String TAG = "GestureDetector";
    private MjpegActivity mjpegActivity;
    private HeadTracker tracker;
    private Car.Direction direction = Car.Direction.STRAIGHT;
    private Car.DrivingMode mode = Car.DrivingMode.BRAKE;
    private ArrayList<String> trackerData = new ArrayList<>();

    public GestureDetector(MjpegActivity mjpegActivity) {
        this.mjpegActivity = mjpegActivity;

        //init head tracking
        tracker = HeadTracker.createFromContext(mjpegActivity);
        tracker.startTracking();
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
            outputStream = mjpegActivity.openFileOutput(fnm, Context.MODE_PRIVATE);

            for (int i = 0; i < trackerData.size(); i++) {
                outputStream.write((trackerData.get(i) + "\n").getBytes());
            }
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "finished");
    }

    @Override
    public void run() {

        //check the current head transform and send the appropriate command
        if (tracker != null) {

            float[] headView = new float[3];
            tracker.getLastHeadView(headView, 0);

            //if looking right/left, send right/left command, etc.
            if (headView[8] > 0.2) direction = Car.Direction.RIGHT;
            else if (headView[8] < -0.2) direction = Car.Direction.LEFT;
            else direction = Car.Direction.STRAIGHT;

            //looking up or down?
            if (headView[9] < -0.2) mode = Car.DrivingMode.BACKWARD;
            else if (headView[9] > 0.2) mode = Car.DrivingMode.FORWARD;
            else mode = Car.DrivingMode.BRAKE;

            //now send command
            switch (direction) {
                case LEFT:
                    mjpegActivity.left(Car.Speed.FAST);
                    break;
                case RIGHT:
                    mjpegActivity.right(Car.Speed.FAST);
                    break;
                case STRAIGHT:
                    mjpegActivity.straight();
                    break;
            }

            Log.d(TAG, direction.name());

            //send driving mode to car
            switch (mode) {
                case FORWARD:
                    mjpegActivity.forward(Car.Speed.SLOW);
                    break;
                case BACKWARD:
                    mjpegActivity.backward(Car.Speed.SLOW);
                    break;
                case BRAKE:
                    mjpegActivity.brake();
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
