package com.example.smartsaver.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class StockDetailActivity extends AppCompatActivity {

    private TextView stockSymbol, stockPrice, stockChange;
    private LineChart lineChart;
    private final String API_KEY = "d09uahpr01qus8rffsfgd09uahpr01qus8rffsg0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        stockSymbol = findViewById(R.id.stockSymbol);
        stockPrice = findViewById(R.id.stockPrice);
        stockChange = findViewById(R.id.stockChange);
        lineChart = findViewById(R.id.lineChart);

        String symbol = getIntent().getStringExtra("stock_code");
        stockSymbol.setText(symbol);

        fetchQuoteData(symbol);
        fetchCandleData(symbol);
    }

    private void fetchQuoteData(String symbol) {
        String url = "https://finnhub.io/api/v1/quote?symbol=" + symbol + "&token=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double current = response.getDouble("c");
                        double open = response.getDouble("o");
                        double change = ((current - open) / open) * 100;

                        stockPrice.setText("Price: ₺" + String.format("%.2f", current));
                        stockChange.setText("Change: " + String.format("%.2f", change) + "%");
                    } catch (JSONException e) {
                        Toast.makeText(this, "Price data error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Price fetch failed", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    private void fetchCandleData(String symbol) {
        // Şu anki zaman (UTC olarak bugünün 00:00:00)
        long to = System.currentTimeMillis() / 1000;
        long from = to - (7 * 24 * 60 * 60); // 7 gün önce

        String url = "https://finnhub.io/api/v1/stock/candle?symbol=" + symbol +
                "&resolution=D&from=" + from + "&to=" + to + "&token=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (!response.has("c") || response.getJSONArray("c").length() == 0) {
                            Toast.makeText(this, "No candle data available", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        JSONArray closes = response.getJSONArray("c");
                        List<Entry> entries = new ArrayList<>();

                        for (int i = 0; i < closes.length(); i++) {
                            float value = (float) closes.getDouble(i);
                            entries.add(new Entry(i, value));
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "7-Day Trend");
                        dataSet.setColor(getResources().getColor(R.color.colorPrimary));
                        dataSet.setCircleRadius(3f);
                        dataSet.setValueTextSize(10f);

                        lineChart.setData(new LineData(dataSet));
                        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        lineChart.getDescription().setText("Closing Prices");
                        lineChart.animateX(1000);
                        lineChart.invalidate();

                    } catch (JSONException e) {
                        Toast.makeText(this, "Chart JSON error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Chart fetch error", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

}
