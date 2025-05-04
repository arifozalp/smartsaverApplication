package com.example.smartsaver.activities;

import android.os.Bundle;
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
    private TextView balanceStat, totalInvestment, totalProfit, positiveCount, negativeCount;
    private Button btnViewTransfers;
    private int userId;
    private static final String BASE_URL = "http://10.0.2.2:3000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_stats);

        // View binding
        balanceStat      = findViewById(R.id.balanceStat);
        totalInvestment  = findViewById(R.id.totalInvestment);
        totalProfit      = findViewById(R.id.totalProfit);
        positiveCount    = findViewById(R.id.positiveCount);
        negativeCount    = findViewById(R.id.negativeCount);
        btnViewTransfers = findViewById(R.id.btnViewTransfers);

        // Intent’ten gelen userId
        userId = getIntent().getIntExtra("user_id", -1);
        if (userId < 0) {
            Toast.makeText(this, "Kullanıcı bilgisi bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Bakiye ve portföyü çek
        fetchBalance();
        fetchPortfolio();

        // Geçmiş transferleri butona bağla
        btnViewTransfers.setOnClickListener(v -> fetchTransactions());
    }

    /** Kullanıcının güncel bakiyesini çeker ve gösterir */
    private void fetchBalance() {
        String url = BASE_URL + "/user_profiles/" + userId;
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        double bal = response.getDouble("balance");
                        balanceStat.setText(
                                String.format(Locale.getDefault(), "Balance: ₺%.2f", bal)
                        );
                    } catch (JSONException e) {
                        Toast.makeText(this, "Bakiye verisi okunamadı", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> Toast.makeText(this, "Bakiye alınamadı", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(req);
    }

    /** Portföy API’sından dönen array’i işleyip ekrana yazar */
    private void fetchPortfolio() {
        String url = BASE_URL + "/portfolio/" + userId;
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                response -> {
                    double totalInv = 0, totalProf = 0;
                    int posCnt = 0, negCnt = 0;
                    for (int i = 0; i < response.length(); i++) {
                        JSONObject obj = response.optJSONObject(i);
                        if (obj == null) continue;
                        double qty      = obj.optDouble("quantity", 0);
                        double avgPrice = obj.optDouble("avg_price", 0);
                        totalInv += qty * avgPrice;
                        // henüz profit bilgisi yok => 0
                        double p = 0;
                        totalProf += p;
                        if (p >= 0) posCnt++; else negCnt++;
                    }
                    totalInvestment.setText(
                            String.format(Locale.getDefault(), "Total Investment: ₺%.2f", totalInv)
                    );
                    totalProfit.setText(
                            String.format(Locale.getDefault(), "Total Profit: ₺%.2f", totalProf)
                    );
                    positiveCount.setText("Profitable Stocks: " + posCnt);
                    negativeCount.setText("Loss Stocks: " + negCnt);
                },
                error -> Toast.makeText(this, "Portföy yüklenemedi", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(req);
    }

    /** Sadece bu kullanıcıya ait transfer geçmişini çeker */
    private void fetchTransactions() {
        String url = BASE_URL + "/transactions/" + userId;
        JsonArrayRequest req = new JsonArrayRequest(Request.Method.GET, url, null,
                this::showTransactionsDialog,
                error -> Toast.makeText(this, "İşlem geçmişi yüklenemedi", Toast.LENGTH_SHORT).show()
        );
        Volley.newRequestQueue(this).add(req);
    }

    /**
     * JSON dizisini “Sent/Received” formatında bir dialog’ta gösterir.
     * Artık hem gönderenin hem de alıcının full_name’lerini kullanıyoruz.
     */
    private void showTransactionsDialog(JSONArray arr) {
        ArrayList<JSONObject> transactionList = new ArrayList<>();

        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null) transactionList.add(o);
        }

        // 📌 Amount'a göre küçükten büyüğe sırala
        transactionList.sort((o1, o2) -> {
            double a1 = o1.optDouble("amount", 0);
            double a2 = o2.optDouble("amount", 0);
            return Double.compare(a1, a2);
        });

        ArrayList<String> lines = new ArrayList<>();

        for (JSONObject o : transactionList) {
            try {
                int from          = o.getInt("user_id");
                int to            = o.getInt("target_user_id");
                double amt        = o.getDouble("amount");
                String date       = o.getString("date");
                String senderName = o.getString("sender_name");
                String targetName = o.getString("target_name");

                String line;
                if (from == userId) {
                    line = String.format(Locale.getDefault(),
                            "Sent ₺%.2f to %s on %s", amt, targetName, date);
                } else {
                    line = String.format(Locale.getDefault(),
                            "Received ₺%.2f from %s on %s", amt, senderName, date);
                }

                lines.add(line);
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
