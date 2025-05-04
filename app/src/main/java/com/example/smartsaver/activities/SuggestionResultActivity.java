package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsaver.R;

public class SuggestionResultActivity extends AppCompatActivity {

    private TextView planSummary, recommendationText, riskLevelBox;
    private Button backButton;

    private String userEmail;
    private double amount;
    private String duration, risk, goal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggestion_result);

        planSummary = findViewById(R.id.planSummary);
        recommendationText = findViewById(R.id.recommendationText);
        riskLevelBox = findViewById(R.id.riskLevelBox);
        backButton = findViewById(R.id.backButton);

        // Gelen verileri al
        Intent intent = getIntent();
        userEmail = intent.getStringExtra("user_email");
        amount = intent.getDoubleExtra("amount", 0);
        duration = intent.getStringExtra("duration");
        risk = intent.getStringExtra("risk");
        goal = intent.getStringExtra("goal");

        // Yeni eklenen hisse bilgileri
        String stockSymbol = intent.getStringExtra("stock_symbol");
        String stockName   = intent.getStringExtra("stock_name");
        double stockPrice  = intent.getDoubleExtra("stock_price", 0);
        String stockNote   = intent.getStringExtra("stock_note");

        // Plan özeti
        planSummary.setText("You plan to invest $" + amount + " for a " + duration.toLowerCase() +
                " term with a " + risk.toLowerCase() + " risk level towards: " + goal + ".");

        // Öneri metni
        String recommendation = "We recommend buying stock \"" + stockName + "\" (" + stockSymbol +
                ") currently priced at $" + stockPrice + ".\n\n" + stockNote;

        recommendationText.setText(recommendation);

        // Risk kutusu renklendirme
        riskLevelBox.setText("Risk Level: " + risk);
        switch (risk.toLowerCase()) {
            case "low":
                riskLevelBox.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "medium":
                riskLevelBox.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "high":
                riskLevelBox.setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
                break;
            default:
                riskLevelBox.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
        }

        // Geri dön butonu
        backButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(this, DashboardActivity.class);
            backIntent.putExtra("user_email", userEmail);
            startActivity(backIntent);
            finish();
        });
    }
}
