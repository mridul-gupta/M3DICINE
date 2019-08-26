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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;


public class AudioActivity extends AppCompatActivity {
    private static final String LOG_TAG = AudioActivity.class.getSimpleName();
    private String[] permissions = {Manifest.permission.RECORD_AUDIO};
    private boolean permissionToRecord = false;
    private int displayWidth;

    private Utils.STATE state = Utils.STATE.READYTORECORD;

    private AudioService audioService;
    private WaveChart mChart = new WaveChart();

    private TextView    textStatus = null;
    private Button      buttonTopStatus = null;
    private LineChart   viewChartAudio = null;
    private View        playHead = null;
    private ImageButton buttonRecordPlay = null;
    private Button      buttonBottom = null;

    public int countdownCounter = Utils.MAX_TIME / 1000;
    private CountDownTimer timer = null;

    private int usedIndex = 0;
    private Handler mHandler = new Handler();


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

        textStatus          = findViewById(R.id.tv_view_name);
        buttonTopStatus     = findViewById(R.id.bt_status);
        viewChartAudio      = findViewById(R.id.chart_audio);
        playHead            = findViewById(R.id.v_playhead);
        buttonRecordPlay    = findViewById(R.id.bt_recordplay);
        buttonBottom        = findViewById(R.id.bt_bottom);

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        displayWidth = displayMetrics.widthPixels;

        buttonRecordPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (state) {
                    case READYTORECORD:
                        startRecording();
                        break;

                    case RECORDING:
                        stopRecording();
                        break;

                    case READYTOPLAY:
                        startPlaying();
                        break;

                    case PLAYING:
                        stopPlaying();
                        break;

                    default:
                        state = Utils.STATE.READYTORECORD;
                        buttonRecordPlay.setBackground(getDrawable(R.drawable.record));
                        Log.e(LOG_TAG, "Wrong state");
                }
            }
        });

        buttonBottom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (state) {
                    case READYTOPLAY:
                    case PLAYING:
                        stopPlaying();
                        stopRecording();
                        break;

                    case RECORDING:
                    case READYTORECORD:
                    default:
                        //Do nothing
                }
            }
        });

        mChart.setupChart(viewChartAudio);
    }

    private void startRecording() {
        state = Utils.STATE.RECORDING;
        buttonRecordPlay.setBackground(getDrawable(R.drawable.stop));
        buttonBottom.setEnabled(false);

        audioService.startRecording();
        mHandler.postDelayed(mTickExecutor, Utils.UI_UPDATE_FREQ);
        uiCountdownTimer();
    }

    private void stopRecording() {
        state = Utils.STATE.READYTORECORD;
        textStatus.setText(R.string.recording_view);
        buttonRecordPlay.setBackground(getDrawable(R.drawable.record));
        buttonTopStatus.setText(R.string.ready);
        buttonBottom.setText(R.string.playback_now);
        buttonBottom.setEnabled(false);

        viewChartAudio.clearValues();
        mChart.setupChart(viewChartAudio);
        audioService.stopRecording();

        usedIndex = 0;
        countdownCounter = Utils.MAX_TIME / 1000;
        mHandler.removeCallbacks(mTickExecutor);
        timer.cancel();
    }

    private void startPlaying() {
        state = Utils.STATE.PLAYING;
        buttonRecordPlay.setBackground(getDrawable(R.drawable.stop));
        playHead.setVisibility(View.VISIBLE);
        audioService.startPlaying();
        mHandler.postDelayed(mPlayTickExecutor, Utils.UI_UPDATE_FREQ);
    }

    private void stopPlaying() {
        state = Utils.STATE.READYTOPLAY;
        buttonRecordPlay.setBackground(getDrawable(R.drawable.play));
        mHandler.removeCallbacks(mPlayTickExecutor);

        playHead.setVisibility(View.GONE);
        playHead.setTranslationX(0);
        audioService.stopPlaying();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        audioService.stopRecording();
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


            //Log.d("Plotting: ","used: "+ usedIndex + ", current: " + currentIndex + ", " + viewChartAudio.getData().getDataSetCount());
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
                buttonTopStatus.setText(String.valueOf(countdownCounter));
                countdownCounter--;
            }

            public void onFinish() {
                audioService.stopRecording();
                state = Utils.STATE.READYTOPLAY;
                textStatus.setText(R.string.playback_view);
                buttonRecordPlay.setBackground(getDrawable(R.drawable.play));
                buttonTopStatus.setText(R.string.ready);
                buttonBottom.setText(R.string.back_rec);
                buttonBottom.setEnabled(true);

            }
        }.start();
    }

    private void updatePlayingUI() {
        int curr = audioService.getPlayProgress();

        playHead.setTranslationX(curr * (displayWidth / 20000f));
    }
}
