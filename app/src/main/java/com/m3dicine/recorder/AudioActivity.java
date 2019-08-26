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

    private AudioService audioService;
    private WaveChart mChart = new WaveChart();

    Utils.STATE state = Utils.STATE.READYTORECORD;

    private boolean permissionToRecord = false;
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};

    ImageButton buttonRecordPlay = null;
    Button buttonTopStatus = null;
    public int counter = 20;

    CountDownTimer timer = null;

    int usedIndex = 0;
    private Handler mHandler = new Handler();

    int displayWidth;

    private Runnable mTickExecutor = new Runnable() {
        @Override
        public void run() {
            tick();
            mHandler.postDelayed(mTickExecutor, Utils.UI_UPDATE_FREQ);
        }
    };

    private Runnable mPlayTickExecutor = new Runnable() {
        @Override
        public void run() {
            updatePlayingUI();
            mHandler.postDelayed(mPlayTickExecutor, Utils.UI_UPDATE_FREQ);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audioService = new AudioService(this);
        ActivityCompat.requestPermissions(this, permissions, Utils.REQUEST_RECORD_AUDIO_PERMISSION);

        buttonRecordPlay = findViewById(R.id.bt_recordplay);
        buttonTopStatus = findViewById(R.id.bt_status);
        LineChart mChartAudio = findViewById(R.id.chart_audio);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;

        buttonRecordPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (state) {
                    case READYTORECORD:
                        state = Utils.STATE.RECORDING;
                        buttonRecordPlay.setBackground(getDrawable(R.drawable.stop));

                        audioService.startRecording();
                        mHandler.postDelayed(mTickExecutor, Utils.UI_UPDATE_FREQ);
                        uiCountdownTimer();
                        break;

                    case RECORDING:
                        state = Utils.STATE.READYTORECORD;
                        buttonRecordPlay.setBackground(getDrawable(R.drawable.record));

                        audioService.stopRecording();
                        mHandler.removeCallbacks(mTickExecutor);
                        timer.cancel();
                        break;

                    case READYTOPLAY:
                        state = Utils.STATE.PLAYING;
                        buttonRecordPlay.setBackground(getDrawable(R.drawable.stop));
                        audioService.startPlaying();
                        mHandler.postDelayed(mPlayTickExecutor, Utils.UI_UPDATE_FREQ);
                        break;

                    case PLAYING:
                        state = Utils.STATE.READYTOPLAY;
                        buttonRecordPlay.setBackground(getDrawable(R.drawable.play));
                        audioService.stopPlaying();
                        break;

                    default:
                        state = Utils.STATE.READYTORECORD;
                        buttonRecordPlay.setBackground(getDrawable(R.drawable.record));
                        Log.e(LOG_TAG, "Wrong state");
                }
            }
        });

        mChart.setupChart(mChartAudio);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Utils.REQUEST_RECORD_AUDIO_PERMISSION) {
            permissionToRecord = grantResults[0] == PackageManager.PERMISSION_GRANTED;
        }
        if (!permissionToRecord) {
            Toast.makeText(this, "No permission to record", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    private void tick() {
        if (state == Utils.STATE.RECORDING) {
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
        timer = new CountDownTimer(Utils.MAX_TIME, 1000) {
            public void onTick(long millisUntilFinished) {
                buttonTopStatus.setText(String.valueOf(counter));
                counter--;
            }

            public void onFinish() {
                audioService.stopRecording();
                state = Utils.STATE.READYTOPLAY;
                buttonRecordPlay.setBackground(getDrawable(R.drawable.play));
                buttonTopStatus.setText(R.string.ready);

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
