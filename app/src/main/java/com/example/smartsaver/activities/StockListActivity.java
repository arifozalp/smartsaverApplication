package com.example.smartsaver.activities;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GestureDetectorCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;
import com.example.smartsaver.adapters.StockAdapter;
import com.example.smartsaver.database.DBHelper;
import com.example.smartsaver.models.Stock;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

public class StockListActivity extends AppCompatActivity {

    private RecyclerView stockRecyclerView;
    private StockAdapter adapter;
    private List<Stock> stockList = new ArrayList<>();

    private DBHelper dbHelper;
    private final String API_KEY = "9UHXBHGRB85774EZ";
    private final String BASE_URL = "https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=";
    private final String[] symbols = {"AAPL", "MSFT", "AMZN", "GOOG", "TSLA", "NVDA", "META", "BABA", "NFLX", "ADBE"};

    private ItemTouchHelper itemTouchHelper;
    private GestureDetectorCompat gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_list);

        dbHelper = new DBHelper(this);
        stockRecyclerView = findViewById(R.id.stockRecyclerView);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StockAdapter(this, stockList, stock -> {
            Intent intent = new Intent(this, StockDetailActivity.class);
            intent.putExtra("stock_code", stock.code);
            intent.putExtra("stock_name", stock.name);
            startActivity(intent);
        });
        stockRecyclerView.setAdapter(adapter);

        initGestureDrag();
        loadStocks();
    }

    /**
     * Gesture‑tabanlı sürükle‑bırak kurulumu
     */
    private void initGestureDrag() {
        // ItemTouchHelper – yalnızca move işlemleri aktif
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                int from = viewHolder.getAdapterPosition();
                int to = target.getAdapterPosition();
                Collections.swap(stockList, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                // swipe pasif
            }

            // Sadece uzun basılıp drag başladığında görsel kaldırma efekti
            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder viewHolder, int actionState) {
                super.onSelectedChanged(viewHolder, actionState);
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
                    viewHolder.itemView.setAlpha(0.7f);
                }
            }

            @Override
            public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1f);
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(stockRecyclerView);

        // GestureDetector – uzun basıyı yakalar ve drag'i başlatır
        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                View child = stockRecyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null) {
                    RecyclerView.ViewHolder holder = stockRecyclerView.getChildViewHolder(child);
                    itemTouchHelper.startDrag(holder);
                }
            }

            // onSingleTapUp true dönmezse longPress çalışmayabilir
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                return true;
            }
        });

        stockRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {
                // no‑op
            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
                // no‑op
            }
        });
    }

    // -----------------------  DATA  ----------------------------

    private void loadStocks() {
        for (String symbol : symbols) {
            if (shouldFetchFromApi(symbol)) {
                fetchStockDataFromApi(symbol);
            } else {
                loadStockFromDatabase(symbol);
            }
        }
    }

    private boolean shouldFetchFromApi(String symbol) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT last_updated FROM stocks WHERE symbol = ?", new String[]{symbol});
        if (cursor.moveToFirst()) {
            String lastUpdated = cursor.getString(0);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            cursor.close();
            return !today.equals(lastUpdated);
        }
        cursor.close();
        return true;
    }

    private void loadStockFromDatabase(String symbol) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT symbol, name, price, change_percent FROM stocks WHERE symbol = ?", new String[]{symbol});
        if (cursor.moveToFirst()) {
            Stock stock = new Stock();
            stock.code = cursor.getString(0);
            stock.name = cursor.getString(1);
            stock.price = cursor.getDouble(2);
            stock.changePercent = cursor.getDouble(3);
            stockList.add(stock);
            adapter.notifyDataSetChanged();
        }
        cursor.close();
    }

    private void fetchStockDataFromApi(String symbol) {
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
                        saveStockToDatabase(stock);

                    } catch (JSONException e) {
                        Toast.makeText(this, "Parse error: " + symbol, Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "API error: " + symbol, Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    private void saveStockToDatabase(Stock stock) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        db.execSQL("REPLACE INTO stocks (symbol, name, price, change_percent, last_updated) VALUES (?, ?, ?, ?, ?)",
                new Object[]{stock.code, stock.name, stock.price, stock.changePercent, today});
    }
}
