package com.m3dicine.recorder;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;


class SoundService {
    private static final String LOG_TAG = SoundService.class.getSimpleName();
    private Context context;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    private String fileName;
    private double lastMax = 100.0d;

    private long start_time = 0;
    private int UPDATE_DELAY = 20; //millisecond

    private Boolean running = false;


    final ArrayList<Double> sounds; //index, amp

    SoundService(Context mContext) {
        this.context = mContext;
        sounds = new ArrayList<>();

        fileName = Objects.requireNonNull(context.getExternalCacheDir()).getAbsolutePath();
        fileName += "/audiorecordtest.3gp";
    }

    void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(fileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
        start_time = System.currentTimeMillis();
        //Toast.makeText(context, "Started Recording", Toast.LENGTH_SHORT).show();

        startUpdateData();
    }

    void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        running = false;

        //Toast.makeText(context, "Stopped Recording", Toast.LENGTH_SHORT).show();
    }

    private void startUpdateData() {
        running = true;
        new Thread(new Runnable() {

            @Override
            public void run() {
                while (running) {
                    Log.d("Added Index: ", "" + sounds.size());
                    int indexTo = (int) ((System.currentTimeMillis() - start_time) / UPDATE_DELAY);

                    for (int i = sounds.size(); i < indexTo; i++) {
                        sounds.add(i, getAmplitudeDb());
                    }

                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private double getAmplitudeDb() {
        return 20.0d * Math.log10(getAmplitude());
    }

    //ToDo: cleanup
    private double getAmplitude() {
        try {
            double maxAmp = (double) this.mRecorder.getMaxAmplitude();
            if (maxAmp > 2.0d) {
                this.lastMax = maxAmp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this.lastMax;
    }

    void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fileName);
            mPlayer.setWakeMode(context.getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mPlayer.prepare();
            mPlayer.start();
            //Toast.makeText(context, "Started Playing", Toast.LENGTH_SHORT).show();

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    finishPlaying();
                }
            });
        } catch (IOException e) {
            //Toast.makeText(context, "Failed to play", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        //Toast.makeText(context, "Stopped Playing", Toast.LENGTH_SHORT).show();
    }

    private void finishPlaying() {
        mPlayer.release();
        mPlayer = null;
        //Toast.makeText(context, "Finished Playing", Toast.LENGTH_SHORT).show();
    }
}
