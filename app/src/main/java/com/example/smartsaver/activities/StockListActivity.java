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

    private int userId;
    private String userEmail;

    private ItemTouchHelper itemTouchHelper;
    private GestureDetectorCompat gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_list);

        userId = getIntent().getIntExtra("user_id", -1);
        userEmail = getIntent().getStringExtra("user_email");

        dbHelper = new DBHelper(this);
        stockRecyclerView = findViewById(R.id.stockRecyclerView);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StockAdapter(this, stockList, stock -> {
            Intent intent = new Intent(this, StockDetailActivity.class);
            intent.putExtra("stock_code", stock.code);
            intent.putExtra("stock_name", stock.name);
            intent.putExtra("user_id", userId);
            intent.putExtra("user_email", userEmail);
            startActivity(intent);
        });
        stockRecyclerView.setAdapter(adapter);

        initGestureDrag();
        loadStocks();
    }

    private void initGestureDrag() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder tgt) {
                int from = vh.getAdapterPosition(), to = tgt.getAdapterPosition();
                Collections.swap(stockList, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder vh, int dir) { }

            @Override
            public void onSelectedChanged(RecyclerView.ViewHolder vh, int state) {
                super.onSelectedChanged(vh, state);
                if (state == ItemTouchHelper.ACTION_STATE_DRAG && vh != null) {
                    vh.itemView.setAlpha(0.7f);
                }
            }

            @Override
            public void clearView(RecyclerView rv, RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh);
                vh.itemView.setAlpha(1f);
            }
        };
        itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(stockRecyclerView);

        gestureDetector = new GestureDetectorCompat(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public void onLongPress(MotionEvent e) {
                View child = stockRecyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child != null) {
                    RecyclerView.ViewHolder vh = stockRecyclerView.getChildViewHolder(child);
                    itemTouchHelper.startDrag(vh);
                }
            }

            @Override public boolean onSingleTapUp(MotionEvent e) { return true; }
        });

        stockRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                gestureDetector.onTouchEvent(e);
                return false;
            }

            @Override public void onTouchEvent(RecyclerView rv, MotionEvent e) { }

            @Override public void onRequestDisallowInterceptTouchEvent(boolean d) { }
        });
    }

    private void loadStocks() {
        for (String sym : symbols) {
            if (shouldFetchFromApi(sym)) {
                fetchStockDataFromApi(sym);
            } else {
                loadStockFromDatabase(sym);
            }
        }
    }

    private boolean shouldFetchFromApi(String symbol) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT last_updated FROM stocks WHERE symbol=?", new String[]{symbol});
        boolean fetch = true;
        if (c.moveToFirst()) {
            String last = c.getString(0);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            fetch = !today.equals(last);
        }
        c.close();
        return fetch;
    }

    private void loadStockFromDatabase(String symbol) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT symbol,name,price,change_percent FROM stocks WHERE symbol=?",
                new String[]{symbol});
        if (c.moveToFirst()) {
            Stock s = new Stock();
            s.code = c.getString(0);
            s.name = c.getString(1);
            s.price = c.getDouble(2);
            s.changePercent = c.getDouble(3);
            stockList.add(s);
            adapter.notifyDataSetChanged();
        }
        c.close();
    }

    private void fetchStockDataFromApi(String symbol) {
        String url = BASE_URL + symbol + "&apikey=" + API_KEY;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                resp -> {
                    try {
                        JSONObject ts = resp.getJSONObject("Time Series (Daily)");
                        Iterator<String> dates = ts.keys();
                        if (!dates.hasNext()) return;
                        String latest = dates.next();
                        double close1 = Double.parseDouble(ts.getJSONObject(latest).getString("4. close"));
                        if (!dates.hasNext()) return;
                        String prev = dates.next();
                        double close2 = Double.parseDouble(ts.getJSONObject(prev).getString("4. close"));
                        double pct = ((close1 - close2) / close2) * 100;

                        Stock s = new Stock();
                        s.code = symbol;
                        s.name = symbol;
                        s.price = close1;
                        s.changePercent = pct;

                        stockList.add(s);
                        adapter.notifyDataSetChanged();
                        saveStockToDatabase(s);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Parse error: " + symbol, Toast.LENGTH_SHORT).show();
                    }
                },
                err -> Toast.makeText(this, "API error: " + symbol, Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(req);
    }

    private void saveStockToDatabase(Stock stock) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        db.execSQL("REPLACE INTO stocks(symbol,name,price,change_percent,last_updated) VALUES(?,?,?,?,?)",
                new Object[]{stock.code, stock.name, stock.price, stock.changePercent, today});
    }
}
