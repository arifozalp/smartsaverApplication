package com.example.smartsaver.activities;

import android.app.AlertDialog;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.*;

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

    private static final String BASE_URL   = "http://10.0.2.2:3000";
    private static final String TRX_URL    = BASE_URL + "/transactions";

    private static final String BUY_URL  = BASE_URL + "/portfolio/buy";
    private static final String SELL_URL = BASE_URL + "/portfolio/sell";
    private static final String TABLE_HIST = "stock_details";
    private static final String TABLE_OWN  = "user_holdings";

    /* ----------------Views------------------------- */
    private TextView stockSymbol, stockPrice, stockChange,
            userBalanceText, ownedSharesText,
            quantityText, totalText;
    private Spinner      rangeSpinner;
    private ToggleButton toggleMode;
    private Button   btnInc, btnDec, btnConfirm;
    private LineChart lineChart;

    /* ----------------Data-------------------------- */
    private DBHelper dbHelper;
    private String   symbol;          // “AAPL” veya “ASELS”
    private int      userId;
    private double   currentPrice = 0;
    private double   balance      = 0;
    private int      ownedShares  = 0;
    private int      quantity     = 0;

    /* ==================================================================== */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);

        bindViews();
        dbHelper = new DBHelper(this);

        symbol  = getIntent().getStringExtra("stock_code");
        userId  = getIntent().getIntExtra("user_id",-1);
        stockSymbol.setText(symbol);

        initSpinner();
        ensureTables();
        fetchUserBalanceAndOwned();
    }

    /* -------------------- View & Spinner ------------------------------- */
    private void bindViews() {
        stockSymbol     = findViewById(R.id.stockSymbol);
        stockPrice      = findViewById(R.id.stockPrice);
        stockChange     = findViewById(R.id.stockChange);
        userBalanceText = findViewById(R.id.userBalance);
        ownedSharesText = findViewById(R.id.ownedShares);
        quantityText    = findViewById(R.id.quantityText);
        totalText       = findViewById(R.id.totalText);

        rangeSpinner    = findViewById(R.id.rangeSpinner);
        toggleMode      = findViewById(R.id.toggleMode);
        btnInc          = findViewById(R.id.btnIncrease);
        btnDec          = findViewById(R.id.btnDecrease);
        btnConfirm      = findViewById(R.id.btnConfirm);
        lineChart       = findViewById(R.id.lineChart);

        toggleMode.setChecked(true);                 // default BUY

        // ① Artır / azalt
        btnInc.setOnClickListener(v -> changeQuantity(+1));
        btnDec.setOnClickListener(v -> changeQuantity(-1));

        // ② Buy <-> Sell geçişinde miktarı sıfırla
        toggleMode.setOnCheckedChangeListener((btn, isBuy) -> {
            quantity = 0;        // hafızadaki miktarı sıfırla
            changeQuantity(0);   // UI’de “0” & “Total: ₺0.00” güncellensin
        });

        // ③ Confirm
        btnConfirm.setOnClickListener(v -> confirmTransaction());
    }

    private void initSpinner() {
        ArrayAdapter<String> sp = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"1W","1M","1Y"});
        sp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rangeSpinner.setAdapter(sp);
        rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, android.view.View v,int pos,long id){
                loadRange(Range.values()[pos]);
            }
            @Override public void onNothingSelected(AdapterView<?> p){}
        });
        rangeSpinner.setSelection(0);
    }

    /* -------------------- SQLite tables -------------------------------- */
    private void ensureTables() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_HIST+"(" +
                "symbol TEXT, date TEXT, close REAL, freq TEXT," +
                "PRIMARY KEY(symbol,date,freq))");

        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE_OWN+"(" +
                "user_id INTEGER, symbol TEXT, quantity INTEGER," +
                "PRIMARY KEY(user_id,symbol))");
    }

    /* -------------------- Balance & Owned ------------------------------ */
    private void fetchUserBalanceAndOwned() {
        /* bakiye sunucudan */
        String balUrl = BASE_URL + "/user_profiles/" + userId;
        Volley.newRequestQueue(this).add(new StringRequest(balUrl,
                r -> {
                    try {
                        balance = new JSONObject(r).getDouble("balance");
                        userBalanceText.setText(String.format(Locale.getDefault(),
                                "Balance: ₺%.2f", balance));
                    } catch (Exception ignore) {}
                }, e -> {}));

        /* owned shares (yerel cache) */
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT quantity FROM "+TABLE_OWN+
                        " WHERE user_id=? AND symbol=?",
                new String[]{String.valueOf(userId), symbol});
        if (c.moveToFirst()) ownedShares = c.getInt(0);
        c.close();

        ownedSharesText.setText("Owned: " + ownedShares);
    }

    /* -------------------- Quantity controls ---------------------------- */
    private void changeQuantity(int delta){
        boolean isBuy = toggleMode.isChecked();
        int max = isBuy ? (int)Math.floor(balance/currentPrice) : ownedShares;
        quantity = Math.max(0, Math.min(max, quantity + delta));

        quantityText.setText(String.valueOf(quantity));
        totalText.setText(String.format(Locale.getDefault(),
                "Total: ₺%.2f", currentPrice * quantity));
    }

    /* -------------------- Confirmation dialog -------------------------- */
    private void confirmTransaction(){
        if(quantity == 0){
            Toast.makeText(this,"Quantity is zero",Toast.LENGTH_SHORT).show();
            return;
        }
        boolean isBuy = toggleMode.isChecked();
        String mode   = isBuy ? "Buy" : "Sell";

        new AlertDialog.Builder(this)
                .setTitle(mode + " " + quantity + " " + symbol + "?")
                .setMessage("Are you sure?")
                .setNegativeButton("Cancel",null)
                .setPositiveButton("Yes",(d,w)-> sendTradeRequest(isBuy))
                .show();
    }

    /* -------------------- REST: buy/sell ------------------------------- */
    private void sendTradeRequest(boolean isBuy) {
        String url = isBuy ? BUY_URL : SELL_URL;

        StringRequest req = new StringRequest(
                Request.Method.POST, url,
                resp -> {
                    // ⇣⇣ local state ⇣⇣
                    if (isBuy)  ownedShares += quantity;
                    else        ownedShares -= quantity;
                    updateHoldingsInDb();
                    updateBalanceAfterTrade();
                    Toast.makeText(this, "Done!", Toast.LENGTH_SHORT).show();
                },
                err -> Toast.makeText(this, "Trade failed", Toast.LENGTH_LONG).show()
        ) {
            @Override
            protected Map<String, String> getParams() {
                double totalCost = quantity * currentPrice;

                Map<String, String> p = new HashMap<>();
                p.put("user_id",        String.valueOf(userId));
                p.put("target_user_id", "");                         // buy/sell’de yok
                p.put("type",           isBuy ? "buy" : "sell");
                p.put("stock_code",     symbol);

                // toplam ₺ tutar – bakiyeden düşülecek / eklenecek miktar
                p.put("amount",   String.format(Locale.US, "%.2f", totalCost));

                // Kac lot aldık / sattık?  <- EKSİK OLAN SATIR
                p.put("quantity", String.valueOf(quantity));

                // işlem fiyatı (birim fiyat) – backend’de raporlamada kullanılabilir
                p.put("price",    String.format(Locale.US, "%.2f", currentPrice));

                p.put("date", new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(new Date()));
                return p;
            }
        };

        Volley.newRequestQueue(this).add(req);
    }
    private void updateBalanceAfterTrade(){
        String url = BASE_URL + "/user_profiles/" + userId;
        Volley.newRequestQueue(this).add(new StringRequest(url,
                r -> {
                    try{
                        balance = new JSONObject(r).getDouble("balance");
                        userBalanceText.setText(String.format(Locale.getDefault(),
                                "Balance: ₺%.2f", balance));
                    }catch(Exception ignore){}

                    ownedSharesText.setText("Owned: " + ownedShares);
                    quantity = 0;
                    changeQuantity(0);       // limitleri ve “Total”i sıfırla
                },
                e -> Toast.makeText(this,"Balance fetch failed",Toast.LENGTH_SHORT).show()));
    }

    private void updateHoldingsInDb(){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("REPLACE INTO "+TABLE_OWN+"(user_id,symbol,quantity) VALUES(?,?,?)",
                new Object[]{userId, symbol, ownedShares});
    }

    /* ====================================================================
                               PRICE & CHART
       ==================================================================== */
    private void loadRange(Range range){
        if(shouldFetch(range)) fetchCsv(range);
        else                   loadFromDb(range);
    }

    private boolean shouldFetch(Range range){
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT MAX(date) FROM "+TABLE_HIST+
                " WHERE symbol=? AND freq=?", new String[]{symbol, range.name()});
        String last=null; if(c.moveToFirst()) last=c.getString(0); c.close();

        String today = new SimpleDateFormat("yyyy-MM-dd",Locale.getDefault()).format(new Date());
        return last==null || !today.equals(last);
    }

    /* ------------- Stooq helpers ------------- */
    private String normSym(){
        return symbol.contains(".") ? symbol.toLowerCase()
                : symbol.toLowerCase()+".us";
    }
    private String stooqUrl(){
        return "https://stooq.com/q/d/l/?s=" + normSym() + "&i=d";
    }

    /* ------------- Fetch CSV & save ----------- */
    private void fetchCsv(Range range){
        Volley.newRequestQueue(this).add(
                new StringRequest(Request.Method.GET, stooqUrl(),
                        csv -> {
                            String[] rows = csv.trim().split("\n");
                            if(rows.length<=2){ lineChart.clear(); return; }

                            int need = range==Range.YEAR?365 : range==Range.MONTH?30 : 7;
                            List<Entry> entries   = new ArrayList<>();
                            List<Float> closes    = new ArrayList<>();
                            SQLiteDatabase db = dbHelper.getWritableDatabase();
                            db.beginTransaction();

                            for(int i=rows.length-1,x=0;i>=1&&x<need;i--,x++){
                                String[] col = rows[i].split(",");
                                String   date= col[0];
                                float close  = Float.parseFloat(col[4]);

                                if(x==0) currentPrice = close;
                                entries.add(new Entry(x,close));
                                closes.add(close);

                                db.execSQL("REPLACE INTO "+TABLE_HIST+
                                                "(symbol,date,close,freq) VALUES(?,?,?,?)",
                                        new Object[]{symbol, date, close, range.name()});
                            }
                            db.setTransactionSuccessful();
                            db.endTransaction();

                            renderChart(entries, closes, range);
                            changeQuantity(0);   // limitleri price’a göre yeniden hesapla
                        },
                        err -> lineChart.setNoDataText("Network error"))
        );
    }

    /* ------------- Load from DB -------------- */
    private void loadFromDb(Range range){
        int limit = range==Range.YEAR?365 : range==Range.MONTH?30 : 7;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT date,close FROM "+TABLE_HIST+
                        " WHERE symbol=? AND freq=? ORDER BY date DESC LIMIT "+limit,
                new String[]{symbol, range.name()});

        List<Entry> entries=new ArrayList<>();
        List<Float> closes =new ArrayList<>();
        int i=0;
        while(c.moveToNext()){
            float close = c.getFloat(1);
            entries.add(new Entry(i++,close));
            closes.add(close);
        }
        c.close();

        if(!closes.isEmpty()) currentPrice = closes.get(0);
        renderChart(entries, closes, range);
        changeQuantity(0);
    }

    /* ------------- Draw chart ---------------- */
    private void renderChart(List<Entry> entries, List<Float> closes, Range range){
        if(entries.isEmpty()){
            lineChart.clear();
            lineChart.setNoDataText("No chart data");
            return;
        }

        if(closes.size()>=2){
            float last = closes.get(0), prev = closes.get(1);
            float pct  = ((last-prev)/prev)*100f;
            stockPrice.setText(String.format(Locale.getDefault(),
                    "Price: ₺%.2f", last));
            stockChange.setText(String.format(Locale.getDefault(),
                    "Change: %.2f%%", pct));
        }

        String label = range==Range.YEAR? "365 Days":
                range==Range.MONTH? "30 Days":"7 Days";

        LineDataSet ds = new LineDataSet(entries, label);
        ds.setValueTextSize(9f);
        ds.setCircleRadius(3f);
        ds.setColor(getResources().getColor(R.color.colorPrimary));

        lineChart.setData(new LineData(ds));
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getDescription().setText("Closing Prices");
        lineChart.animateX(600);
        lineChart.invalidate();
    }
}