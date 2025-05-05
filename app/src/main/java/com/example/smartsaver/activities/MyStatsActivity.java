package com.example.smartsaver.activities;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MyStatsActivity extends AppCompatActivity {

    /* ---------- UI ---------- */
    private TextView balanceStat, totalInvestment, totalProfit, positiveCount, negativeCount;
    private Button   btnViewTransfers, btnViewHoldings;

    /* ---------- Data ---------- */
    private int userId;
    private static final String BASE_URL = "http://10.0.2.2:3000";

    private static class StockItem {
        String code;
        double qty, avgPrice, curPrice = 0;
    }
    private final ArrayList<StockItem> holdings = new ArrayList<>();

    /* ---------- Periodic refresh (5 s) ---------- */
    private final Handler refreshHandler = new Handler(Looper.getMainLooper());
    private final Runnable refreshTask   = new Runnable() {
        @Override public void run() {
            fetchPortfolio();
            refreshHandler.postDelayed(this, 5_000);   // 5 s
        }
    };

    /* =================================================================== */
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stats);

        balanceStat      = findViewById(R.id.balanceStat);
        totalInvestment  = findViewById(R.id.totalInvestment);
        totalProfit      = findViewById(R.id.totalProfit);
        positiveCount    = findViewById(R.id.positiveCount);
        negativeCount    = findViewById(R.id.negativeCount);
        btnViewTransfers = findViewById(R.id.btnViewTransfers);
        btnViewHoldings  = findViewById(R.id.btnViewHoldings);

        userId = getIntent().getIntExtra("user_id", -1);
        if (userId < 0) { finish(); return; }

        fetchBalance();
        fetchPortfolio();

        btnViewTransfers.setOnClickListener(v -> fetchTransactions());
        btnViewHoldings .setOnClickListener(v -> showHoldingsDialog());
    }

    /* ---------- Lifecycle ---------- */
    @Override protected void onResume() {
        super.onResume();
        refreshHandler.post(refreshTask);
    }
    @Override protected void onPause() {
        super.onPause();
        refreshHandler.removeCallbacks(refreshTask);
    }

    /* ---------- Balance ---------- */
    private void fetchBalance() {
        String url = BASE_URL + "/user_profiles/" + userId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                r -> {
                    try {
                        double bal = r.getDouble("balance");
                        balanceStat.setText(String.format(Locale.getDefault(),
                                "Balance: $%.2f", bal));
                    } catch (JSONException ignored) {}
                },
                e -> Toast.makeText(this,"Bakiye alınamadı",Toast.LENGTH_SHORT).show());
        Volley.newRequestQueue(this).add(req);
    }

    /* ---------- Portfolio & prices ---------- */
    private void fetchPortfolio() {
        String url = BASE_URL + "/portfolio/" + userId;
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                arr -> {
                    holdings.clear();
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject o = arr.optJSONObject(i);
                        if (o == null) continue;
                        StockItem s   = new StockItem();
                        s.code        = o.optString("stock_code","?").toUpperCase(Locale.ROOT);
                        s.qty         = o.optDouble("quantity",0);
                        s.avgPrice    = o.optDouble("avg_price",0);
                        holdings.add(s);
                    }
                    if (holdings.isEmpty()) { updateStatsViews(); return; }

                    final int[] done = {0};
                    for (StockItem s : holdings) {
                        fetchCurrentPrice(s.code, p -> {
                            s.curPrice = p;
                            if (++done[0] == holdings.size()) updateStatsViews();
                        });
                    }
                },
                e -> Toast.makeText(this,"Portföy yüklenemedi",Toast.LENGTH_SHORT).show());
        Volley.newRequestQueue(this).add(req);
    }

    private interface PriceCallback { void onPrice(double p); }
    private void fetchCurrentPrice(String code, PriceCallback cb) {
        String norm = code.contains(".") ? code.toLowerCase()
                : code.toLowerCase() + ".us";
        String url  = "https://stooq.com/q/l/?s=" + norm + "&f=cl";
        Volley.newRequestQueue(this).add(
                new com.android.volley.toolbox.StringRequest(url,
                        csv -> {
                            try {
                                String[] p = csv.trim().split(",");
                                cb.onPrice(Double.parseDouble(p[1]));
                            } catch (Exception ex) { cb.onPrice(0); }
                        },
                        err -> cb.onPrice(0))
        );
    }

    private void updateStatsViews() {
        double totalInv=0, totalProf=0;
        int posCnt=0, negCnt=0;
        for (StockItem s : holdings) {
            double cur   = s.curPrice>0 ? s.curPrice : s.avgPrice;
            double cost  = s.qty*s.avgPrice;
            double value = s.qty*cur;
            double prof  = value - cost;
            totalInv  += cost;
            totalProf += prof;
            if (prof>=0) posCnt++; else negCnt++;
        }
        totalInvestment.setText(String.format(Locale.getDefault(),
                "Total Investment: $%.2f", totalInv));
        totalProfit.setText(String.format(Locale.getDefault(),
                "Total Profit: $%.2f", totalProf));
        positiveCount.setText("Profitable Stocks: " + posCnt);
        negativeCount.setText("Loss Stocks: " + negCnt);
    }

    /* ---------- Holdings dialog ---------- */
    private void showHoldingsDialog() {
        if (holdings.isEmpty()) { Toast.makeText(this,"No holdings",Toast.LENGTH_SHORT).show(); return; }
        ArrayList<String> lines = new ArrayList<>();
        for (StockItem s : holdings) {
            double cur = s.curPrice>0 ? s.curPrice : s.avgPrice;
            double prof= (cur - s.avgPrice) * s.qty;
            lines.add(String.format(Locale.getDefault(),
                    "%s • %,.0f pcs • Avg $%.2f • Now $%.2f • %s$%.2f",
                    s.code, s.qty, s.avgPrice, cur,
                    prof>=0?"+":"-", Math.abs(prof)));
        }
        new AlertDialog.Builder(this)
                .setTitle("Current Holdings")
                .setItems(lines.toArray(new String[0]), null)
                .setPositiveButton("Close", null)
                .show();
    }

    /* ---------- Transfers ---------- */
    private void fetchTransactions() {
        String url = BASE_URL + "/transactions/" + userId;
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                this::showTransactionsDialog,
                e -> Toast.makeText(this,"İşlem geçmişi yüklenemedi",Toast.LENGTH_SHORT).show());
        Volley.newRequestQueue(this).add(req);
    }

    private void showTransactionsDialog(JSONArray arr) {
        ArrayList<JSONObject> list = new ArrayList<>();
        for (int i=0;i<arr.length();i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o!=null) list.add(o);
        }
        list.sort((a,b)->Double.compare(a.optDouble("amount",0),
                b.optDouble("amount",0)));

        ArrayList<String> lines = new ArrayList<>();
        for (JSONObject o : list) {
            try {
                int from  = o.getInt("user_id");
                double amt= o.getDouble("amount");
                String d  = o.getString("date");
                String sn = o.getString("sender_name");
                String tn = o.getString("target_name");
                String l  = (from==userId)
                        ? String.format(Locale.getDefault(),
                        "Sent $%.2f to %s on %s", amt, tn, d)
                        : String.format(Locale.getDefault(),
                        "Received $%.2f from %s on %s", amt, sn, d);
                lines.add(l);
            } catch (JSONException ignored) {}
        }
        if (lines.isEmpty()) lines.add("No transactions found.");
        new AlertDialog.Builder(this)
                .setTitle("Transfer History")
                .setItems(lines.toArray(new String[0]), null)
                .setPositiveButton("Close", null)
                .show();
    }
}
