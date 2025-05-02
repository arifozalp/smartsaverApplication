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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StockDetailActivity extends AppCompatActivity {

    private TextView stockSymbol, stockPrice, stockChange;
    private LineChart lineChart;

    private final String API_KEY = "9UHXBHGRB85774EZ";

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

        fetchStockData(symbol);
    }

    private void fetchStockData(String symbol) {
        String url = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=" + symbol + "&apikey=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject timeSeries = response.getJSONObject("Time Series (Daily)");
                        Iterator<String> dates = timeSeries.keys();

                        List<Entry> entries = new ArrayList<>();
                        List<Float> closePrices = new ArrayList<>();
                        int index = 0;

                        while (dates.hasNext() && index < 7) { // Son 7 gün
                            String date = dates.next();
                            JSONObject dayData = timeSeries.getJSONObject(date);
                            float close = Float.parseFloat(dayData.getString("4. close"));
                            entries.add(new Entry(index, close));
                            closePrices.add(close);
                            index++;
                        }

                        if (closePrices.size() >= 2) {
                            float last = closePrices.get(0);
                            float previous = closePrices.get(1);
                            float change = ((last - previous) / previous) * 100;

                            stockPrice.setText("Price: $" + String.format("%.2f", last));
                            stockChange.setText("Change: " + String.format("%.2f", change) + "%");
                        }

                        LineDataSet dataSet = new LineDataSet(entries, "Last 7 Days");
                        dataSet.setColor(getResources().getColor(R.color.colorPrimary));
                        dataSet.setCircleRadius(3f);
                        dataSet.setValueTextSize(10f);

                        lineChart.setData(new LineData(dataSet));
                        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
                        lineChart.getDescription().setText("Closing Prices");
                        lineChart.animateX(1000);
                        lineChart.invalidate();

                    } catch (JSONException e) {
                        Toast.makeText(this, "Data parse error", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "API error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}
