package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;
import com.example.smartsaver.adapters.StockAdapter;
import com.example.smartsaver.models.Stock;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class StockListActivity extends AppCompatActivity {

    private RecyclerView stockRecyclerView;
    private StockAdapter adapter;
    private List<Stock> stockList = new ArrayList<>();

    private final String API_KEY = "9UHXBHGRB85774EZ";
    private final String BASE_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=";
    private final String[] symbols = {"AAPL", "MSFT", "AMZN", "GOOG"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_list);

        stockRecyclerView = findViewById(R.id.stockRecyclerView);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StockAdapter(this, stockList, stock -> {
            Intent intent = new Intent(this, StockDetailActivity.class);
            intent.putExtra("stock_code", stock.code);
            intent.putExtra("stock_name", stock.name);
            startActivity(intent);
        });

        stockRecyclerView.setAdapter(adapter);
        fetchAllStocks();
    }

    private void fetchAllStocks() {
        for (String symbol : symbols) {
            fetchStockData(symbol);
        }
    }

    private void fetchStockData(String symbol) {
        String url = BASE_URL + symbol + "&apikey=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONObject timeSeries = response.getJSONObject("Time Series (Daily)");
                        Iterator<String> dates = timeSeries.keys();

                        if (!dates.hasNext()) return;
                        String latestDate = dates.next();
                        JSONObject latestData = timeSeries.getJSONObject(latestDate);
                        double latestClose = Double.parseDouble(latestData.getString("4. close"));

                        if (!dates.hasNext()) return;
                        String previousDate = dates.next();
                        JSONObject previousData = timeSeries.getJSONObject(previousDate);
                        double previousClose = Double.parseDouble(previousData.getString("4. close"));

                        double changePercent = ((latestClose - previousClose) / previousClose) * 100;

                        Stock stock = new Stock();
                        stock.code = symbol;
                        stock.name = symbol;
                        stock.price = latestClose;
                        stock.changePercent = changePercent;

                        stockList.add(stock);
                        adapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        Toast.makeText(this, "Parse error: " + symbol, Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "API error: " + symbol, Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }
}
