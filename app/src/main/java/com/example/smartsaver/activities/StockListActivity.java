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
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;
import com.example.smartsaver.adapters.StockAdapter;
import com.example.smartsaver.database.DBHelper;
import com.example.smartsaver.models.Stock;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Stooq tabanlı hisse listesi – Alpha Vantage bağımlılığı kaldırıldı.
 */
public class StockListActivity extends AppCompatActivity {

    private RecyclerView       stockRecyclerView;
    private StockAdapter       adapter;
    private final List<Stock>  stockList = new ArrayList<>();

    private DBHelper           dbHelper;

    /* ---------------------------------------------------- */
    private static final String[] SYMBOLS = {
            "AAPL.US","MSFT.US","AMZN.US","GOOG.US","TSLA.US",
            "NVDA.US","META.US","BABA.US","NFLX.US","ADBE.US"
            // BIST eklemek isterseniz: "ASELS.TR","TUPRS.TR" ...
    };
    /* ---------------------------------------------------- */

    private int    userId;
    private String userEmail;

    private ItemTouchHelper       itemTouchHelper;
    private GestureDetectorCompat gestureDetector;

    /* ============================ lifecycle ================================ */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_list);

        userId    = getIntent().getIntExtra("user_id",   -1);
        userEmail = getIntent().getStringExtra("user_email");

        dbHelper = new DBHelper(this);

        stockRecyclerView = findViewById(R.id.stockRecyclerView);
        stockRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new StockAdapter(this, stockList, stock -> {
            Intent i = new Intent(this, StockDetailActivity.class);
            i.putExtra("stock_code",  stock.code);
            i.putExtra("stock_name",  stock.name);
            i.putExtra("user_id",     userId);
            i.putExtra("user_email",  userEmail);
            startActivity(i);
        });
        stockRecyclerView.setAdapter(adapter);

        initGestureDrag();
        loadStocks();
    }

    /* ============================ drag & drop ============================== */
    private void initGestureDrag() {
        ItemTouchHelper.SimpleCallback cb = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder tgt) {
                int from = vh.getAdapterPosition(), to = tgt.getAdapterPosition();
                Collections.swap(stockList, from, to);
                adapter.notifyItemMoved(from, to);
                return true;
            }
            @Override public void onSwiped(RecyclerView.ViewHolder vh, int dir) { }
            @Override public void onSelectedChanged(RecyclerView.ViewHolder vh, int st) {
                super.onSelectedChanged(vh, st);
                if (st == ItemTouchHelper.ACTION_STATE_DRAG && vh != null) vh.itemView.setAlpha(0.7f);
            }
            @Override public void clearView(RecyclerView rv, RecyclerView.ViewHolder vh) {
                super.clearView(rv, vh); vh.itemView.setAlpha(1f);
            }
        };
        itemTouchHelper = new ItemTouchHelper(cb);
        itemTouchHelper.attachToRecyclerView(stockRecyclerView);

        gestureDetector = new GestureDetectorCompat(this,new GestureDetector.SimpleOnGestureListener() {
            @Override public void onLongPress(MotionEvent e) {
                View child = stockRecyclerView.findChildViewUnder(e.getX(), e.getY());
                if (child!=null) itemTouchHelper.startDrag(stockRecyclerView.getChildViewHolder(child));
            }
            @Override public boolean onSingleTapUp(MotionEvent e) { return true; }
        });
        stockRecyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                gestureDetector.onTouchEvent(e); return false;
            }
            @Override public void onTouchEvent(RecyclerView rv, MotionEvent e) { }
            @Override public void onRequestDisallowInterceptTouchEvent(boolean d) { }
        });
    }

    /* ============================ veri akışı ================================= */
    private void loadStocks() {
        for (String sym : SYMBOLS) {
            if (shouldFetchFromApi(sym)) fetchStockData(sym);
            else                         loadStockFromDatabase(sym);
        }
    }

    private boolean shouldFetchFromApi(String symbol) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT last_updated FROM stocks WHERE symbol=?", new String[]{symbol});
        boolean fetch = true;
        if (c.moveToFirst()) {
            String last = c.getString(0);
            String today= new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
            fetch = !today.equals(last);
        }
        c.close();
        return fetch;
    }

    /* ============================ Stooq fetch ============================== */
    private String stooqUrl(String sym) {
        return "https://stooq.com/q/d/l/?s=" + sym.toLowerCase() + "&i=d";
    }

    private void fetchStockData(String symbol) {
        StringRequest req = new StringRequest(
                Request.Method.GET, stooqUrl(symbol),
                csv -> {
                    String[] rows = csv.trim().split("\n");
                    if (rows.length <= 2) {                         // başlık + tek satırdan az
                        Toast.makeText(this,"Stooq: no data "+symbol,Toast.LENGTH_SHORT).show();
                        return;
                    }
                    /* --- CSV eski→yeni -> sondan başla --- */
                    String[] latest = rows[rows.length-1].split(",");
                    String[] prev   = rows[rows.length-2].split(",");

                    double close1 = Double.parseDouble(latest[4]);
                    double close2 = Double.parseDouble(prev[4]);
                    double pct    = ((close1-close2)/close2)*100;

                    Stock s = new Stock();
                    s.code          = symbol;
                    s.name          = symbol;
                    s.price         = close1;
                    s.changePercent = pct;

                    stockList.add(s);
                    adapter.notifyDataSetChanged();
                    saveStockToDatabase(s);
                },
                e -> Toast.makeText(this,"Network error "+symbol,Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(req);
    }

    /* ============================ DB işlemleri ============================= */
    private void loadStockFromDatabase(String symbol) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT symbol,name,price,change_percent FROM stocks WHERE symbol=?",
                new String[]{symbol});
        if (c.moveToFirst()) {
            Stock s = new Stock();
            s.code          = c.getString(0);
            s.name          = c.getString(1);
            s.price         = c.getDouble(2);
            s.changePercent = c.getDouble(3);
            stockList.add(s);
            adapter.notifyDataSetChanged();
        }
        c.close();
    }

    private void saveStockToDatabase(Stock stock) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String today = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
        db.execSQL("REPLACE INTO stocks(symbol,name,price,change_percent,last_updated) VALUES(?,?,?,?,?)",
                new Object[]{stock.code, stock.name, stock.price, stock.changePercent, today});
    }
}
