package com.m3dicine.recorder;

import android.graphics.Color;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;

public class WaveChart {
    private LineChart mChartAudio = null;

    private int MAX_TIME = 20000; //millisecond
    private int UPDATE_DELAY = 50; //millisecond
    private int MAX_X_ENTRIES = MAX_TIME / UPDATE_DELAY;

    public void setupChart(LineChart mChartAudio) {
        this.mChartAudio = mChartAudio;
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
        xaxis.setDrawLabels(true);
        xaxis.setDrawGridLines(true);
        xaxis.setPosition(XAxis.XAxisPosition.TOP);
        xaxis.setValueFormatter(new DefaultAxisValueFormatter(0));
        xaxis.setAxisMaximum(MAX_X_ENTRIES);


        YAxis axisLeft = mChartAudio.getAxisLeft();
        axisLeft.setAxisMaximum(100.0f);
        axisLeft.setAxisMinimum(-100.0f);
        axisLeft.setDrawLabels(true);

        mChartAudio.setData(new LineData());
        mChartAudio.invalidate();

        mChartAudio.getAxisRight().setEnabled(false);


        LineData lineData = mChartAudio.getData();
        LineDataSet setXSound = createSet("u", "u", Color.rgb(240, 99, 99));
        LineDataSet setXSound2 = createSet("d", "d", Color.rgb(240, 99, 99));
        setXSound.setDrawFilled(true);
        setXSound.setFillColor(Color.rgb(240, 99, 99));
        lineData.addDataSet(setXSound);

        setXSound2.setDrawFilled(true);
        setXSound2.setFillColor(Color.rgb(240, 99, 99));
        lineData.addDataSet(setXSound2);
    }

    public void addEntry(float xValue, float yValue, int dataSetIndex) {
        LineData data = mChartAudio.getData();
        if (data != null) {
            data.addEntry(new Entry(xValue, yValue), dataSetIndex);
        }
        invalidateChart(mChartAudio);
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

    public void invalidateChart(LineChart chart) {
        chart.notifyDataSetChanged();
        chart.getData().notifyDataChanged();
        chart.invalidate();

        chart.notifyDataSetChanged();
        chart.getData().notifyDataChanged();
        chart.invalidate();
    }

}
