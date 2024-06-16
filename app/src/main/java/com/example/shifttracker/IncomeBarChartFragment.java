package com.example.shifttracker;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IncomeBarChartFragment extends Fragment {
    private BarChart barChart;

    public IncomeBarChartFragment() {
        super(R.layout.fragment_income_barchart);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income_barchart, container, false);

        barChart = view.findViewById(R.id.barChart);
        populateBarChart(FirebaseManager.calculateIncomeForLastSixMonths());

        return view;
    }

    private class CurrencyValueFormatter extends ValueFormatter {
        @Override
        public String getBarLabel(BarEntry barEntry) {
            return "$" + String.format("%.0f", barEntry.getY());
        }
    }

    private void populateBarChart(Map<String, Float> incomeData) {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int index = 0;

        for (Map.Entry<String, Float> entry : incomeData.entrySet()) {
            entries.add(new BarEntry(index, entry.getValue()));
            labels.add(entry.getKey());
            index++;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Monthly Income");
        dataSet.setColors(ColorTemplate.PASTEL_COLORS);
        dataSet.setValueTextSize(14f); // Set text size of bar's height
        dataSet.setValueFormatter(new CurrencyValueFormatter());

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.75f); // set custom bar width

        XAxis xAxis = barChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM_INSIDE);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
//        xAxis.setTextColor(Color.BLACK);
        xAxis.setTextSize(10f);
        xAxis.setDrawAxisLine(false);
        xAxis.setDrawGridLines(false);

        barChart.getAxisLeft().setEnabled(false); // Hide left Y-axis
        barChart.getAxisRight().setEnabled(false); // Hide right Y-axis

        barChart.setFitBars(true); // make the x-axis fit exactly all bars
        barChart.setData(barData);
        barChart.getLegend().setEnabled(false); // Remove the legend

        barChart.getDescription().setEnabled(false);
        barChart.setDrawGridBackground(false); // Remove background grid
        barChart.setDrawBorders(false); // Remove chart borders

        barChart.setTouchEnabled(false);

        barChart.invalidate(); // refresh
    }
}
