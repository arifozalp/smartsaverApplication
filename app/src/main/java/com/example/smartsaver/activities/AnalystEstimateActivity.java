package com.example.smartsaver.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AnalystEstimateActivity extends AppCompatActivity {

    private TextView revenueRangeText, epsText, analystCountText;
    private final String API_KEY = "jv5olHHmCXakPx29j6I3pRiiM5iXMfSK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyst_estimate);

        revenueRangeText = findViewById(R.id.revenueRangeText);
        epsText = findViewById(R.id.epsText);
        analystCountText = findViewById(R.id.analystCountText);

        String symbol = getIntent().getStringExtra("stock_code");
        fetchEstimateData(symbol);
    }

    private void fetchEstimateData(String symbol) {
        String url = "https://financialmodelingprep.com/api/v3/analyst-estimates?symbol="
                + symbol + "&period=annual&apikey=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray estimates = response.getJSONArray("estimates");
                        if (estimates.length() > 0) {
                            JSONObject estimate = estimates.getJSONObject(0);
                            double low = estimate.getDouble("estimatedRevenueLow");
                            double high = estimate.getDouble("estimatedRevenueHigh");
                            double eps = estimate.getDouble("estimatedEpsAvg");
                            int analystCount = estimate.getInt("numberAnalystEstimatedRevenue");

                            revenueRangeText.setText("Revenue Range: $" + formatBillion(low) + " - $" + formatBillion(high));
                            epsText.setText("Estimated EPS (avg): " + eps);
                            analystCountText.setText("Analyst Count: " + analystCount);
                        } else {
                            Toast.makeText(this, "No analyst data available", Toast.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        Toast.makeText(this, "JSON parsing error", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                },
                error -> Toast.makeText(this, "API error", Toast.LENGTH_SHORT).show()
        );

        Volley.newRequestQueue(this).add(request);
    }

    private String formatBillion(double value) {
        return String.format("%.2fB", value / 1_000_000_000);
    }
}
