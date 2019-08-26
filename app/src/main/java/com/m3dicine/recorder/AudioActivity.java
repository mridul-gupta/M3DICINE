package com.m3dicine.recorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;


public class AudioActivity extends AppCompatActivity {
    private static final String LOG_TAG = AudioActivity.class.getSimpleName();

    WaveChart mChart = new WaveChart();
    public AudioService audioService;

    public enum STATE {
        READYTORECORD,
        RECORDING,
        READYTOPLAY,
        PLAYING
    }


    private int MAX_TIME = 20000; //millisecond
    private int UPDATE_DELAY = 50; //millisecond

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    STATE state = STATE.READYTORECORD;

    private boolean permissionToRecord = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    ImageButton button = null;
    Button top_button = null;
    public int counter = 20;

    CountDownTimer timer = null;

    int usedIndex = 0;
    private Handler mHandler = new Handler();

    int displayWidth;

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            tick();
            mHandler.postDelayed(mTickExecutor, UPDATE_DELAY);
        }
    };

    private Runnable mPlayTickExecutor = new Runnable() {
        @Override
        public void run() {
            updatePlayingUI();
            mHandler.postDelayed(mPlayTickExecutor, UPDATE_DELAY);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioService = new AudioService(this);
        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        button = findViewById(R.id.bt_action);
        top_button = findViewById(R.id.bt_top_button);
        LineChart mChartAudio = findViewById(R.id.chart_audio);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (state) {
                    case READYTORECORD:
                        state = STATE.RECORDING;
                        button.setBackground(getDrawable(R.drawable.stop));

                        audioService.startRecording();
                        mHandler.postDelayed(mTickExecutor, UPDATE_DELAY);
                        uiCountdownTimer();
                        break;

                    case RECORDING:
                        state = STATE.READYTORECORD;
                        button.setBackground(getDrawable(R.drawable.record));

                        audioService.stopRecording();
                        mHandler.removeCallbacks(mTickExecutor);
                        timer.cancel();
                        break;

                    case READYTOPLAY:
                        state = STATE.PLAYING;
                        button.setBackground(getDrawable(R.drawable.stop));
                        audioService.startPlaying();
                        mHandler.postDelayed(mPlayTickExecutor, UPDATE_DELAY);
                        break;

                    case PLAYING:
                        state = STATE.READYTOPLAY;
                        button.setBackground(getDrawable(R.drawable.play));
                        audioService.stopPlaying();
                        break;

                    default:
                        state = STATE.READYTORECORD;
                        button.setBackground(getDrawable(R.drawable.record));
                        Log.e(LOG_TAG, "Wrong state");
                }
            }
        });

        mChart.setupChart(mChartAudio);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecord) {
            Toast.makeText(this, "No permission to record", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void tick() {
        if (state == STATE.RECORDING) {
            int currentIndex = audioService.amplitudes.size();


            //Log.d("Plotting: ","used: "+ usedIndex + ", current: " + currentIndex);
            for (int i = usedIndex; i < currentIndex; i++) {
                double amplitude = audioService.amplitudes.get(i);
                //Log.d("Voice Recorder: ","amplitude: "+ amplitude + ", " + (current_time - start_time));
                mChart.addEntry(i, (float) amplitude - 10.0f, 0); //first dataset
                mChart.addEntry(i, (float) -amplitude + 10.0f, 1); //second dataset
            }
            usedIndex = currentIndex;
        }
    }

    private void uiCountdownTimer() {
        timer = new CountDownTimer(MAX_TIME, 1000) {
            public void onTick(long millisUntilFinished) {
                top_button.setText(String.valueOf(counter));
                counter--;
            }

            public void onFinish() {
                audioService.stopRecording();
                state = STATE.READYTOPLAY;
                button.setBackground(getDrawable(R.drawable.play));
                top_button.setText(R.string.ready);

            }
        }.start();
    }

    private void updatePlayingUI() {
        View playHead = findViewById(R.id.v_playhead);
        playHead.setVisibility(View.VISIBLE);
        int curr = audioService.getPlayProgress();

        playHead.setTranslationX(curr * (displayWidth / 20000f));
    }
}
