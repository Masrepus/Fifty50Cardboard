package com.masrepus.fifty50.cardboard;

/**
 * Created by samuel on 21.01.16.
 */
public class Car {
    public enum PinState {LOW, HIGH;
        public static PinState parse(String s) {
            return (s.toUpperCase().contentEquals("LOW")) ? LOW : HIGH;
        }
    }

    public enum Speed {SLOW, FAST}

    public enum Direction {LEFT, RIGHT, STRAIGHT}

    public enum DrivingMode {FORWARD, BACKWARD, BRAKE}
}
