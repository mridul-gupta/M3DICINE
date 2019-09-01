package com.m3dicine.recorder;

import android.content.Context;

import java.util.Objects;

final class Utils {

    public enum STATE {
        READYTORECORD,
        RECORDING,
        READYTOPLAY,
        PLAYING
    }


    static int MAX_TIME = 20000; //millisecond
    static int UI_UPDATE_FREQ = 50; //millisecond

    static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    static String getOutputFileName(Context context) {
        return Objects.requireNonNull(context.getExternalCacheDir()).getAbsolutePath() + "/audiorecordtest.3gp";
    }
}
