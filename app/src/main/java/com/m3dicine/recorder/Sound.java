package com.m3dicine.recorder;


public class Sound {
    public double db = 30.0d;
    public volatile long time = 0;


    public void update(double value) {
        this.db = value;
    }
}
