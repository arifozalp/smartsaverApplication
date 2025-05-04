package com.example.smartsaver.activities;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
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
import java.util.HashMap;
import java.util.Map;

public class StockDetailActivity extends AppCompatActivity {

    private enum Range { WEEK, MONTH, YEAR }

    private TextView  stockSymbol, stockPrice, stockChange, userBalanceText;
    private Spinner   rangeSpinner;
    private LineChart lineChart;
    private EditText  inputQuantity;
    private TextView  totalCostText, ownedQuantityText;
    private Button    btnBuy, btnSell;

    private DBHelper  dbHelper;
    private int       userId;
    private String    symbol;
    private double    currentPrice = 0;

    private static final String API_KEY  = "9UHXBHGRB85774EZ";
    private static final String BASE_URL = "http://10.0.2.2:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        // View binding
        stockSymbol       = findViewById(R.id.stockSymbol);
        stockPrice        = findViewById(R.id.stockPrice);
        stockChange       = findViewById(R.id.stockChange);
        userBalanceText   = findViewById(R.id.userBalance);
        lineChart         = findViewById(R.id.lineChart);
        rangeSpinner      = findViewById(R.id.rangeSpinner);
        inputQuantity     = findViewById(R.id.inputQuantity);
        totalCostText     = findViewById(R.id.totalCostText);
        ownedQuantityText = findViewById(R.id.ownedQuantityText);
        btnBuy            = findViewById(R.id.btnBuy);
        btnSell           = findViewById(R.id.btnSell);

        dbHelper = new DBHelper(this);

        // Intent'ten gelenler
        symbol = getIntent().getStringExtra("stock_code");
        userId = getIntent().getIntExtra("user_id", -1);
        stockSymbol.setText(symbol);

        // Spinner kurulumu
        ArrayAdapter<String> spAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, new String[]{"1W","1M","1Y"}
        );
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rangeSpinner.setAdapter(spAdapter);
        rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                loadRange(Range.values()[position]);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        rangeSpinner.setSelection(0);

        // API’dan bakiye ve portföy bilgilerini çek
        fetchUserBalance();
        fetchOwnedQuantity();

        // Miktar girince toplam tutar güncellensin
        inputQuantity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                String qs = s.toString();
                if (qs.isEmpty() || currentPrice <= 0) {
                    totalCostText.setText("Total: ₺0.00");
                    return;
                }
                try {
                    double qty = Double.parseDouble(qs);
                    double total = qty * currentPrice;
                    totalCostText.setText(String.format(Locale.getDefault(),"Total: ₺%.2f", total));
                } catch (NumberFormatException e) {
                    totalCostText.setText("Total: ₺0.00");
                }
            }
        });

        // Buy düğmesi
        btnBuy.setOnClickListener(v -> {
            String qs = inputQuantity.getText().toString();
            if (qs.isEmpty()) {
                Toast.makeText(this,"Enter quantity",Toast.LENGTH_SHORT).show();
                return;
            }
            int qty;
            try { qty = Integer.parseInt(qs); }
            catch (NumberFormatException e) {
                Toast.makeText(this,"Invalid quantity",Toast.LENGTH_SHORT).show();
                return;
            }
            if (qty <= 0) {
                Toast.makeText(this,"Quantity > 0",Toast.LENGTH_SHORT).show();
                return;
            }
            sendBuyRequest(qty);
        });

        // Sell düğmesi
        btnSell.setOnClickListener(v -> {
            String qs = inputQuantity.getText().toString();
            if (qs.isEmpty()) {
                Toast.makeText(this,"Enter quantity",Toast.LENGTH_SHORT).show();
                return;
            }
            int qty;
            try { qty = Integer.parseInt(qs); }
            catch (NumberFormatException e) {
                Toast.makeText(this,"Invalid quantity",Toast.LENGTH_SHORT).show();
                return;
            }
            if (qty <= 0) {
                Toast.makeText(this,"Quantity > 0",Toast.LENGTH_SHORT).show();
                return;
            }
            sendSellRequest(qty);
        });
    }

    // API’dan bakiye çek
    private void fetchUserBalance() {
        if (userId < 0) return;
        String url = BASE_URL + "/user_profiles/" + userId;
        Volley.newRequestQueue(this).add(new JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    try {
                        double bal = resp.getDouble("balance");
                        userBalanceText.setText(
                                String.format(Locale.getDefault(),"Balance: ₺%.2f", bal)
                        );
                    } catch (JSONException e) {
                        Toast.makeText(this,"Balance parse error",Toast.LENGTH_SHORT).show();
                    }
                },
                err -> Toast.makeText(this,"Balance fetch failed",Toast.LENGTH_SHORT).show()
        ));
    }

    // API’dan sahip olunan miktarı çek
    private void fetchOwnedQuantity() {
        if (userId < 0) return;
        String url = BASE_URL + "/portfolio/" + userId + "/" + symbol;
        Volley.newRequestQueue(this).add(new JsonObjectRequest(
                Request.Method.GET, url, null,
                resp -> {
                    int owned = resp.optInt("quantity", 0);
                    ownedQuantityText.setText("Owned: " + owned);
                },
                err -> ownedQuantityText.setText("Owned: 0")
        ));
    }

    // Buy isteği
    private void sendBuyRequest(int qty) {
        String url = BASE_URL + "/portfolio/buy";
        Volley.newRequestQueue(this).add(new StringRequest(
                Request.Method.POST, url,
                resp -> {
                    Toast.makeText(this,"Bought "+qty+" shares",Toast.LENGTH_SHORT).show();
                    fetchUserBalance();
                    fetchOwnedQuantity();
                },
                err -> Toast.makeText(this,"Buy failed",Toast.LENGTH_SHORT).show()
        ) {
            @Override protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("user_id",    String.valueOf(userId));
                p.put("stock_code", symbol);
                p.put("quantity",   String.valueOf(qty));
                p.put("price",      String.valueOf(currentPrice));
                return p;
            }
        });
    }

    // Sell isteği
    private void sendSellRequest(int qty) {
        String url = BASE_URL + "/portfolio/sell";
        Volley.newRequestQueue(this).add(new StringRequest(
                Request.Method.POST, url,
                resp -> {
                    Toast.makeText(this,"Sold "+qty+" shares",Toast.LENGTH_SHORT).show();
                    fetchUserBalance();
                    fetchOwnedQuantity();
                },
                err -> Toast.makeText(this,"Sell failed",Toast.LENGTH_SHORT).show()
        ) {
            @Override protected Map<String,String> getParams() {
                Map<String,String> p = new HashMap<>();
                p.put("user_id",    String.valueOf(userId));
                p.put("stock_code", symbol);
                p.put("quantity",   String.valueOf(qty));
                p.put("price",      String.valueOf(currentPrice));
                return p;
            }
        });
    }

    private void loadRange(Range range) { /* … önceki kodunuz */ }

    private void ensureTable() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS stock_details (" +
                        " symbol TEXT, date TEXT, close REAL, freq TEXT DEFAULT 'WEEK'," +
                        " PRIMARY KEY(symbol,date,freq)" +
                        ")"
        );
        Cursor c = db.rawQuery("PRAGMA table_info(stock_details)", null);
        boolean hasFreq = false;
        while (c.moveToNext()) {
            if ("freq".equalsIgnoreCase(c.getString(1))) {
                hasFreq = true;
                break;
            }
        }
        c.close();
        if (!hasFreq) {
            db.execSQL("ALTER TABLE stock_details ADD COLUMN freq TEXT DEFAULT 'WEEK'");
        }
    }

    private boolean shouldFetchFromApi(Range range) {
        ensureTable();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cur = db.rawQuery(
                "SELECT MAX(date) FROM stock_details WHERE symbol=? AND freq=?",
                new String[]{symbol, range.name()}
        );
        String last = null;
        if (cur.moveToFirst()) last = cur.getString(0);
        cur.close();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        return last == null || !today.equals(last);
    }

    private void fetchStockData(Range range) {
        String function;
        int limit;
        switch (range) {
            case YEAR:  function = "TIME_SERIES_MONTHLY"; limit = 12; break;
            case MONTH: function = "TIME_SERIES_WEEKLY";  limit = 4;  break;
            default:    function = "TIME_SERIES_DAILY";   limit = 7;
        }

        String url = "https://www.alphavantage.co/query?function="
                + function + "&symbol=" + symbol + "&apikey=" + API_KEY;

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        String key = function.contains("MONTHLY") ? "Monthly Time Series"
                                : function.contains("WEEKLY")  ? "Weekly Time Series"
                                : "Time Series (Daily)";
                        JSONObject series = response.getJSONObject(key);
                        Iterator<String> dates = series.keys();

                        List<Entry> entries = new ArrayList<>();
                        List<Float> closes  = new ArrayList<>();

                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        db.beginTransaction();
                        int i = 0;
                        while (dates.hasNext() && i < limit) {
                            String date = dates.next();
                            float latestClose = Float.parseFloat(
                                    series.getJSONObject(date).getString("4. close")
                            );

                            // → burada güncel fiyata aktarıyoruz:
                            currentPrice = latestClose;

                            entries.add(new Entry(i, latestClose));
                            closes.add(latestClose);

                            db.execSQL(
                                    "REPLACE INTO stock_details(symbol,date,close,freq) VALUES(?,?,?,?)",
                                    new Object[]{symbol, date, latestClose, range.name()}
                            );
                            i++;
                        }
                        db.setTransactionSuccessful();
                        db.endTransaction();

                        renderChart(entries, closes, range);

                    } catch (JSONException e) {
                        Toast.makeText(this, "Data parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "API error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(req);
    }



    private void loadStockFromDatabase(Range range) {
        ensureTable();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int limit = (range == Range.YEAR) ? 12 : (range == Range.MONTH) ? 4 : 7;
        Cursor cur = db.rawQuery(
                "SELECT date, close FROM stock_details WHERE symbol=? AND freq=? ORDER BY date DESC LIMIT " + limit,
                new String[]{symbol, range.name()}
        );

        List<Entry> entries = new ArrayList<>();
        List<Float> closes  = new ArrayList<>();
        int i = 0;
        while (cur.moveToNext()) {
            float close = cur.getFloat(1);
            entries.add(new Entry(i, close));
            closes.add(close);
            i++;
        }
        cur.close();
        renderChart(entries, closes, range);
    }

    private void renderChart(List<Entry> entries, List<Float> closes, Range range) {
        if (closes.size() >= 2) {
            float last = closes.get(0), prev = closes.get(1);
            float change = ((last - prev) / prev) * 100;
            stockPrice.setText(
                    String.format(Locale.getDefault(),"Price: ₺%.2f", last)
            );
            stockChange.setText(
                    String.format(Locale.getDefault(),"Change: %.2f%%", change)
            );
        }

        String label = (range == Range.YEAR)  ? "12 Months"
                : (range == Range.MONTH) ? "4 Weeks"
                : "7 Days";

        LineDataSet ds = new LineDataSet(entries, label);
        ds.setColor(getResources().getColor(R.color.colorPrimary));
        ds.setCircleRadius(3f);
        ds.setValueTextSize(9f);

        lineChart.setData(new LineData(ds));
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getDescription().setText("Closing Prices");
        lineChart.animateX(800);
        lineChart.invalidate();
    }
}
