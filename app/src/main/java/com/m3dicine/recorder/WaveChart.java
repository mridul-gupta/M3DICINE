package com.m3dicine.recorder;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.DefaultAxisValueFormatter;

class WaveChart {
    private LineChart mChartAudio = null;

    private int MAX_TIME = 20000; //millisecond
    private int UPDATE_DELAY = 20; //millisecond
    private int MAX_X_ENTRIES = MAX_TIME / UPDATE_DELAY;

    void setupChart(LineChart mChartAudio) {
        this.mChartAudio = mChartAudio;
        mChartAudio.setDrawGridBackground(false);
        mChartAudio.setTouchEnabled(false);
        mChartAudio.setHighlightPerTapEnabled(false);
        mChartAudio.setDescription(null);
        mChartAudio.setDragEnabled(false);
        mChartAudio.setTouchEnabled(false);
        mChartAudio.setScaleEnabled(false);
        mChartAudio.setScaleYEnabled(false);
        mChartAudio.setViewPortOffsets(0, 0, 0, 0);
        mChartAudio.setPinchZoom(false);
        mChartAudio.setDrawBorders(false);
        mChartAudio.setScaleXEnabled(false);
        mChartAudio.getLegend().setEnabled(false);
        mChartAudio.setVisibleXRangeMaximum(MAX_X_ENTRIES);

        XAxis xaxis = mChartAudio.getXAxis();
        xaxis.setEnabled(true);
        xaxis.setDrawLabels(false);
        xaxis.setDrawGridLines(false);
        xaxis.setDrawAxisLine(false);
        xaxis.setValueFormatter(new DefaultAxisValueFormatter(0));
        xaxis.setAxisMaximum(MAX_X_ENTRIES);


        YAxis axisLeft = mChartAudio.getAxisLeft();
        axisLeft.setAxisMaximum(100.0f);
        axisLeft.setAxisMinimum(-100.0f);
        axisLeft.setDrawAxisLine(false);
        axisLeft.setDrawLabels(false);
        axisLeft.setDrawGridLines(false);
        axisLeft.setDrawZeroLine(true);

        mChartAudio.setData(new LineData());
        mChartAudio.invalidate();

        mChartAudio.getAxisRight().setEnabled(false);


        LineData lineData = mChartAudio.getData();
        LineDataSet setXSound = createSet("u", "u", R.color.blue);
        setXSound.setDrawFilled(true);
        setXSound.setFillColor(R.color.blue);
        lineData.addDataSet(setXSound);

        LineDataSet setXSound2 = createSet("d", "d", R.color.blue);
        setXSound2.setDrawFilled(true);
        setXSound2.setFillColor(R.color.blue);
        lineData.addDataSet(setXSound2);
    }

    void addEntry(float xValue, float yValue, int dataSetIndex) {
        LineData data = mChartAudio.getData();
        if (data != null) {
            data.addEntry(new Entry(xValue, yValue), dataSetIndex);
        }
        invalidateChart(mChartAudio);
    }

    private LineDataSet createSet(String label, String setName, int color) {
        LineDataSet set = new LineDataSet(null, "x");

        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setLineWidth(0.5f);
        set.setLabel(label);
        set.setColor(color);
        set.setDrawCircles(false);
        set.setHighlightEnabled(false);
        set.setDrawValues(false);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        //set.setMode(LineDataSet.Mode.CUBIC_BEZIER);
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
