package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;
import com.example.smartsaver.adapters.StockAdapter;
import com.example.smartsaver.models.Stock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StockListActivity extends AppCompatActivity {

    private RecyclerView stockRecyclerView;
    private StockAdapter adapter;
    private List<Stock> stockList = new ArrayList<>();

    private final String API_KEY = "&apikey=jv5olHHmCXakPx29j6I3pRiiM5iXMfSK";
    private final String BASE_URL = "https://financialmodelingprep.com/api/v3/quote/";
    private final String symbols = "AAPL,MSFT,AMZN,GOOG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_list);

        stockRecyclerView = findViewById(R.id.stockRecyclerView);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StockAdapter(this, stockList, stock -> {
            // tıklanınca detay sayfasına geçmek istersen:
            Intent intent = new Intent(this, StockDetailActivity.class);
            intent.putExtra("stock_code", stock.code);
            intent.putExtra("stock_name", stock.name);
            startActivity(intent);
        });

        stockRecyclerView.setAdapter(adapter);
        fetchStockData();
    }

    private void fetchStockData() {
        String url = BASE_URL + symbols + "?apikey=" + API_KEY;

        JsonArrayRequest request = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            Stock stock = new Stock();
                            stock.code = obj.getString("symbol");
                            stock.name = obj.getString("name");
                            stock.price = obj.getDouble("price");
                            stock.changePercent = obj.getDouble("changesPercentage");

                            stockList.add(stock);
                        }
                        adapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        Toast.makeText(this, "JSON parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "API error: " + error.getMessage(), Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }
}
