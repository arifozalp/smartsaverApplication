package com.example.smartsaver.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;
import com.example.smartsaver.database.DBHelper;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Gösterim aralıkları:
 *  - 1 Hafta  (daily, 7 nokta)
 *  - 1 Ay     (weekly, 4 nokta)
 *  - 1 Yıl    (monthly, 12 nokta)
 */
public class StockDetailActivity extends AppCompatActivity {

    private enum Range {WEEK, MONTH, YEAR}

    private TextView stockSymbol, stockPrice, stockChange;
    private LineChart lineChart;
    private Spinner rangeSpinner;
    private DBHelper dbHelper;

    private final String API_KEY = "9UHXBHGRB85774EZ";
    private String symbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        stockSymbol = findViewById(R.id.stockSymbol);
        stockPrice = findViewById(R.id.stockPrice);
        stockChange = findViewById(R.id.stockChange);
        lineChart = findViewById(R.id.lineChart);
        rangeSpinner = findViewById(R.id.rangeSpinner); // Spinner layoutta eklendi
        dbHelper = new DBHelper(this);

        symbol = getIntent().getStringExtra("stock_code");
        stockSymbol.setText(symbol);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new String[]{"1W", "1M", "1Y"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rangeSpinner.setAdapter(adapter);

        rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Range range = Range.values()[position];
                loadRange(range);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        // Varsayılan 1W
        rangeSpinner.setSelection(0);
    }

    private void loadRange(Range range) {
        if (shouldFetchFromApi(range)) {
            fetchStockData(range);
        } else {
            loadStockFromDatabase(range);
        }
    }

    /**
     * Tablo yoksa oluşturur; eski sürümde "freq" sütunu eksikse ALTER TABLE ekler.
     */
    private void ensureTable() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // 1) Tabloyu (güncel şema ile) oluşturmaya çalış
        db.execSQL("CREATE TABLE IF NOT EXISTS stock_details (" +
                "symbol TEXT, " +
                "date   TEXT, " +
                "close  REAL, " +
                "freq   TEXT DEFAULT 'WEEK', " +
                "PRIMARY KEY(symbol,date,freq))");

        // 2) Eğer tablo eskiden var ama "freq" sütunu yoksa → ALTER TABLE
        try (Cursor c = db.rawQuery("PRAGMA table_info(stock_details)", null)) {
            boolean hasFreq = false;
            while (c.moveToNext()) {
                if ("freq".equalsIgnoreCase(c.getString(1))) {
                    hasFreq = true;
                    break;
                }
            }
            if (!hasFreq) {
                db.execSQL("ALTER TABLE stock_details ADD COLUMN freq TEXT DEFAULT 'WEEK'");
            }
        }
    }

    private boolean shouldFetchFromApi(Range range) {
        ensureTable();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT MAX(date) FROM stock_details WHERE symbol = ? AND freq = ?", new String[]{symbol, range.name()});
        if (cursor.moveToFirst()) {
            String lastUpdated = cursor.getString(0);
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            cursor.close();
            return lastUpdated == null || !today.equals(lastUpdated);
        }
        cursor.close();
        return true;
    }

    // ---------- API ----------
    private void fetchStockData(Range range) {
        String function;
        int limit;
        switch (range) {
            case YEAR:
                function = "TIME_SERIES_MONTHLY"; // 12 ay al
                limit = 12;
                break;
            case MONTH:
                function = "TIME_SERIES_WEEKLY";  // 4 hafta al
                limit = 4;
                break;
            default:
                function = "TIME_SERIES_DAILY";    // 7 gün al
                limit = 7;
        }
        String url = "https://www.alphavantage.co/query?function=" + function + "&symbol=" + symbol + "&apikey=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        String key = function.contains("MONTHLY") ? "Monthly Time Series" : function.contains("WEEKLY") ? "Weekly Time Series" : "Time Series (Daily)";
                        JSONObject series = response.getJSONObject(key);
                        Iterator<String> dates = series.keys();

                        List<Entry> entries = new ArrayList<>();
                        List<Float> closes = new ArrayList<>();
                        int idx = 0;

                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.beginTransaction();
                        while (dates.hasNext() && idx < limit) {
                            String date = dates.next();
                            float close = Float.parseFloat(series.getJSONObject(date).getString("4. close"));
                            entries.add(new Entry(idx, close));
                            closes.add(close);
                            db.execSQL("REPLACE INTO stock_details (symbol,date,close,freq) VALUES (?,?,?,?)", new Object[]{symbol, date, close, range.name()});
                            idx++;
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();

                        renderChart(entries, closes, range);
                    } catch (JSONException e) {
                        Toast.makeText(this, "Parse error", Toast.LENGTH_SHORT).show();
                    }
                }, err -> Toast.makeText(this, "API error", Toast.LENGTH_SHORT).show());

        Volley.newRequestQueue(this).add(request);
    }

    // ---------- DB ----------
    private void loadStockFromDatabase(Range range) {
        ensureTable();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int limit = range == Range.YEAR ? 12 : range == Range.MONTH ? 4 : 7;
        Cursor cursor = db.rawQuery("SELECT date, close FROM stock_details WHERE symbol = ? AND freq = ? ORDER BY date DESC LIMIT " + limit, new String[]{symbol, range.name()});
        List<Entry> entries = new ArrayList<>();
        List<Float> closes = new ArrayList<>();
        int idx = 0;
        while (cursor.moveToNext()) {
            float close = cursor.getFloat(1);
            entries.add(new Entry(idx, close));
            closes.add(close);
            idx++;
        }
        cursor.close();
        renderChart(entries, closes, range);
    }

    // ---------- UI ----------
    private void renderChart(List<Entry> entries, List<Float> closes, Range range) {
        if (closes.size() >= 2) {
            float last = closes.get(0);
            float prev = closes.get(1);
            float change = ((last - prev) / prev) * 100;
            stockPrice.setText("Price: $" + String.format("%.2f", last));
            stockChange.setText("Change: " + String.format("%.2f", change) + "%");
        }
        String label = range == Range.YEAR ? "12 Months" : range == Range.MONTH ? "4 Weeks" : "7 Days";
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(getResources().getColor(R.color.colorPrimary));
        dataSet.setCircleRadius(3f);
        dataSet.setValueTextSize(9f);
        lineChart.setData(new LineData(dataSet));
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getDescription().setText("Closing Prices");
        lineChart.animateX(800);
        lineChart.invalidate();
    }
}
