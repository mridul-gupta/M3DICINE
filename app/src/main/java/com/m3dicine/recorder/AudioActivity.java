package com.m3dicine.recorder;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;


public class AudioActivity extends AppCompatActivity {
    private LineChart mChartAudio;
    private LineDataSet setXSound;
    private LineDataSet setXSound2;

    public enum STATE {
        READYTORECORD,
        RECORDING,
        READYTOPLAY,
        PLAYING
    }

    private int MAX_TIME = 10000; //millisecond
    private int UPDATE_DELAY = 10; //millisecond
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
    public int counter = 10;
    public int milliCounter = 0;

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
                                top_button.setText("Ready");

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

        setupChart();
    }


    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

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
        milliCounter += 1;

        if (mRecorder != null) {
            int amplitude = mRecorder.getMaxAmplitude();
            //Log.d("Voice Recorder: ","amplitude: "+ amplitude);
            addEntry(milliCounter, amplitude%100, 0); //first dataset

            invalidateChart(mChartAudio);
        }
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

    private void setupChart() {
        mChartAudio = findViewById(R.id.chart_audio);
        mChartAudio.setDrawGridBackground(false);
        mChartAudio.setTouchEnabled(true);
        mChartAudio.setHighlightPerTapEnabled(false);
        mChartAudio.setDescription(new Description());
        mChartAudio.setDragEnabled(false);
        mChartAudio.setTouchEnabled(false);
        mChartAudio.setScaleEnabled(true);
        mChartAudio.setScaleYEnabled(true);
        mChartAudio.setExtraLeftOffset(-8.0f);
        mChartAudio.setPinchZoom(false);
        mChartAudio.setScaleXEnabled(false);
        mChartAudio.getLegend().setEnabled(false);
        mChartAudio.setVisibleXRangeMaximum(MAX_X_ENTRIES);

        XAxis xaxis = mChartAudio.getXAxis();
        xaxis.setEnabled(true);
        xaxis.setDrawLabels(false);
        xaxis.setDrawGridLines(true);
        xaxis.setPosition(XAxis.XAxisPosition.TOP);
        xaxis.setValueFormatter(new DefaultAxisValueFormatter(0));
        xaxis.setAxisMaximum(MAX_X_ENTRIES);


        YAxis axisLeft = mChartAudio.getAxisLeft();
        axisLeft.setAxisMaxValue(100.0f);
        axisLeft.setAxisMinValue(-100.0f);
        axisLeft.setDrawLabels(true);

        mChartAudio.setData(new LineData());
        mChartAudio.invalidate();

        mChartAudio.getAxisRight().setEnabled(false);


        LineData lineData = mChartAudio.getData();
        setXSound = createSet("u", "u", Color.rgb(240, 99, 99));
        setXSound2 = createSet("d", "d", Color.rgb(240, 99, 99));
        setXSound.setDrawFilled(true);
        setXSound.setFillColor(Color.rgb(240, 99, 99));
        lineData.addDataSet(setXSound);

        setXSound2.setDrawFilled(true);
        setXSound2.setFillColor(Color.rgb(240, 99, 99));
        lineData.addDataSet(setXSound2);
    }

    private void addEntry(float xValue, float yValue, int dataSetIndex) {
        LineData data = mChartAudio.getData();
        if (data != null) {
            data.addEntry(new Entry(xValue, yValue), dataSetIndex);
        }
        data.notifyDataChanged();
    }

    private LineDataSet createSet (String label, String setName, int color) {
        LineDataSet set = new LineDataSet(null, "x");

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(0.5f);
        set.setLabel(label);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        return set;
    }

    private void invalidateChart(LineChart chart) {
        chart.notifyDataSetChanged();
        chart.getData().notifyDataChanged();
        chart.invalidate();

        chart.notifyDataSetChanged();
        chart.getData().notifyDataChanged();
        chart.invalidate();
    }
}
