package com.m3dicine.recorder;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.io.IOException;
import java.util.Locale;

import static android.media.MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED;


public class RecorderActivity extends AppCompatActivity {
    private LineChart mChartAudio;
    private LineDataSet setUp;
    private LineDataSet setDown;

    public enum STATE {
        READYTORECORD,
        RECORDING,
        READYTOPLAY,
        PLAYING
    }

    private static final String LOG_TAG = "AudioRecordTest";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static String fileName = null;

    STATE state = STATE.READYTORECORD;
    private MediaRecorder recorder = null;
    private MediaPlayer player = null;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    ImageButton button = null;
    Button top_button = null;
    public int counter = 20;

    CountDownTimer timer = null;

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

                        timer = new CountDownTimer(20000, 1000){
                            public void onTick(long millisUntilFinished){
                                top_button.setText(String.valueOf(counter));
                                counter--;
                            }
                            public  void onFinish(){
                                recorder.stop();
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
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recorder.setOutputFile(fileName);
        recorder.setMaxDuration(20000);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        recorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {

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
            recorder.prepare();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }

        recorder.start();
    }

    private void stopRecording() {
        recorder.stop();
        recorder.release();
        recorder = null;
    }

    private void startPlaying() {
        player = new MediaPlayer();
        try {
            player.setDataSource(fileName);
            player.prepare();
            player.start();
        } catch (IOException e) {
            Log.e(LOG_TAG, "prepare() failed");
        }
    }

    private void stopPlaying() {
        player.release();
        player = null;
    }

    private void invalidateChart(LineChart chart) {
        chart.notifyDataSetChanged();
        chart.getData().notifyDataChanged();
        chart.invalidate();

        chart.notifyDataSetChanged();
        chart.getData().notifyDataChanged();
        chart.invalidate();
    }

    private void setupChartDefaults(LineChart chart) {
        chart.getXAxis().setDrawLabels(false);
        chart.getXAxis().setDrawGridLines(true);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setDrawGridLines(false);
        chart.setViewPortOffsets(-40.0f, 0.0f, 0.0f, 0.0f);
        chart.getLegend().setEnabled(false);

        chart.setDrawGridBackground(false);
        chart.setNoDataText("Not supported on your device.");

        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setPinchZoom(false);

        chart.setTouchEnabled(false);
        chart.setHighlightPerTapEnabled(false);
        chart.setDescription(new Description());
        chart.getDescription().setText("");
        chart.setScaleYEnabled(false);
        chart.setExtraLeftOffset(-12.0f);
        chart.setScaleXEnabled(false);
    }

    private LineDataSet createSet(String label, String setName, int color) {
        LineDataSet set = new LineDataSet(null, "x");

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(1.3f);
        set.setLabel(label);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        //set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        return set;
    }

    private void setupSound() {
        this.mChartAudio = findViewById(R.id.chart_audio);
        setupChartDefaults(this.mChartAudio);
        YAxis leftAxis = this.mChartAudio.getAxisLeft();

        leftAxis.setAxisMaxValue(100.0f);
        leftAxis.setAxisMinValue(-100.0f);
        leftAxis.setDrawLabels(false);
        this.mChartAudio.getAxisRight().setEnabled(false);
        //this.mChartAudio.setVisibleXRangeMaximum(120.0f);
        this.mChartAudio.setData(new LineData());
        this.mChartAudio.setGridBackgroundColor(Color.rgb(240, 99, 99));
        LineData data = this.mChartAudio.getData();

        LineDataSet setXSound = createSet("dbplus", "dbplus", Color.rgb(240, 99, 99));
        setXSound.setDrawFilled(true);
        setXSound.setFillColor(Color.rgb(240, 99, 99));
        data.addDataSet(setXSound);

        LineDataSet setXSound2 = createSet("dbminus", "dbminus", Color.rgb(240, 99, 99));
        setXSound2.setDrawFilled(true);
        setXSound2.setFillColor(Color.rgb(240, 99, 99));
        data.addDataSet(setXSound2);


        this.mChartAudio.invalidate();
    }


    private void addChartEntry(float xValue, float yValue, int upDown, LineChart chart, int maxEntries) {
        LineData data = chart.getData();
        if (data != null) {
            ILineDataSet set = data.getDataSetByIndex(upDown);
            if (set.getEntryCount() >= maxEntries) {
                set.removeFirst();
            }

            data.addEntry(new Entry(xValue, yValue), upDown);


            String formattedNumber = String.format(Locale.US, "%.2f", new Object[]{Double.valueOf((double) yValue)});
            if (yValue >= 10.0f) {
                formattedNumber = String.format(Locale.US, "%.1f", new Object[]{Double.valueOf((double) yValue)});
            }
            if (yValue >= 0.0f) {
                formattedNumber = formattedNumber + '+';
            }
            switch (upDown) {
                case 0:
                    this.setUp.setLabel("x: " + formattedNumber);
                    return;
                case 1:
                    this.setDown.setLabel("y: " + formattedNumber);
                    return;
                default:
                    return;
            }
        }
        data.notifyDataChanged();
    }

}
