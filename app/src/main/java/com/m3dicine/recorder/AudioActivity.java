package com.m3dicine.recorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.IOException;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;


public class AudioActivity extends AppCompatActivity {

    WaveChart mChart = new WaveChart();

    public enum STATE {
        READYTORECORD,
        RECORDING,
        READYTOPLAY,
        PLAYING
    }

    private long start_time = 0;
    private long current_time = 0;
    private double lastMax = 100.0d;

    private int MAX_TIME = 20000; //millisecond
    private int UPDATE_DELAY = 50; //millisecond
    private int MAX_X_ENTRIES = MAX_TIME / UPDATE_DELAY;

    private static final String LOG_TAG = "AudioActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;

    STATE state = STATE.READYTORECORD;
    private MediaRecorder mRecorder = null;
    private MediaPlayer mPlayer = null;

    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    ImageButton button = null;
    Button top_button = null;
    public int counter = 20;

    CountDownTimer timer = null;

    private Handler mHandler = new Handler();

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            tick();
            mHandler.postDelayed(mTickExecutor, UPDATE_DELAY);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fileName = getExternalCacheDir().getAbsolutePath();
        fileName += "/audiorecordtest.3gp";
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        button = findViewById(R.id.bt_action);
        top_button = findViewById(R.id.bt_top_button);
        LineChart mChartAudio = findViewById(R.id.chart_audio);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (state) {
                    case READYTORECORD:
                        state = STATE.RECORDING;
                        button.setBackground(getDrawable(R.drawable.stop));

                        startRecording();

                        timer = new CountDownTimer(MAX_TIME, 1000){
                            public void onTick(long millisUntilFinished){
                                top_button.setText(String.valueOf(counter));
                                counter--;
                            }
                            public  void onFinish(){
                                stopRecording();
                                state = STATE.READYTOPLAY;
                                button.setBackground(getDrawable(R.drawable.play));
                                top_button.setText(R.string.ready);

                            }
                        }.start();

                        break;

                    case RECORDING:
                        state = STATE.READYTORECORD;
                        button.setBackground(getDrawable(R.drawable.record));

                        stopRecording();
                        timer.cancel();
                        break;

                    case READYTOPLAY:
                        state = STATE.PLAYING;
                        button.setBackground(getDrawable(R.drawable.stop));
                        startPlaying();
                        break;

                    case PLAYING:
                        state = STATE.READYTOPLAY;
                        button.setBackground(getDrawable(R.drawable.play));
                        stopPlaying();
                        break;

                    default:
                        state = STATE.READYTORECORD;
                        button.setBackground(getDrawable(R.drawable.record));
                }
            }
        });

        mChart.setupChart(mChartAudio);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecordAccepted ) {
            Toast.makeText(this, "No permission to record", Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void startRecording() {
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setOutputFile(fileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        mRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

            @Override
            public void onInfo(MediaRecorder mediaRecorder, int what, int i1) {
                if (what == MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    mediaRecorder.stop();
                    state = STATE.READYTOPLAY;
                    button.setBackground(getDrawable(R.drawable.play));
                }
            }
        });

        try {
            mRecorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRecorder.start();
        start_time = System.currentTimeMillis();
        current_time = System.currentTimeMillis();
        Toast.makeText(this, "Started Recording", Toast.LENGTH_SHORT).show();

        mHandler.postDelayed(mTickExecutor, UPDATE_DELAY);
    }

    private void stopRecording() {
        mRecorder.stop();
        mRecorder.release();
        mRecorder = null;
        mHandler.removeCallbacks(mTickExecutor);
        Toast.makeText(this, "Stopped Recording", Toast.LENGTH_SHORT).show();
    }

    private void tick() {
        if (mRecorder != null) {
            double amplitude = getAmplitudeDb();
            current_time = System.currentTimeMillis();
            //Log.d("Voice Recorder: ","amplitude: "+ amplitude + ", " + (current_time - start_time));
            mChart.addEntry((int)((current_time - start_time)/UPDATE_DELAY), (float)amplitude, 0); //first dataset
            mChart.addEntry((int)((current_time - start_time)/UPDATE_DELAY), (float)-amplitude, 1); //second dataset
        }
    }

    private double getAmplitudeDb() {
        return 20.0d * Math.log10(getAmplitude());
    }

    //ToDo: cleanup
    private double getAmplitude() {
        if (this.mRecorder == null) {
            return this.lastMax;
        }

        double maxAmp = (double) this.mRecorder.getMaxAmplitude();
        if (maxAmp > 2.0d) {
            this.lastMax = maxAmp;
        }
        return this.lastMax;
    }

    private void startPlaying() {
        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(fileName);
            mPlayer.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
            mPlayer.prepare();
            mPlayer.start();
            Toast.makeText(this, "Started Playing", Toast.LENGTH_SHORT).show();

            mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    finishPlaying();
                }
            });
        } catch (IOException e) {
            Toast.makeText(this, "Failed to play", Toast.LENGTH_SHORT).show();
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        mPlayer.release();
        mPlayer = null;
        Toast.makeText(this, "Stopped Playing", Toast.LENGTH_SHORT).show();
    }

    private void finishPlaying() {
        mPlayer.release();
        mPlayer = null;
        Toast.makeText(this, "Finished Playing", Toast.LENGTH_SHORT).show();
    }
}
