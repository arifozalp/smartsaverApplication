package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsaver.R;
import com.example.smartsaver.utils.RiskEvaluator;

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

        // Plan özetini yaz
        planSummary.setText("You plan to invest ₺" + amount + " for a " + duration.toLowerCase() +
                " term with a " + risk.toLowerCase() + " risk level towards: " + goal + ".");

        // RiskEvaluator sınıfıyla öneriyi al
        String recommendation = RiskEvaluator.generateRecommendation(duration, risk, amount);
        recommendationText.setText(recommendation);

        // Risk kutusu renklendirme
        riskLevelBox.setText("Risk Level: " + risk);
        switch (RiskEvaluator.determineRiskColor(risk)) {
            case "green":
                riskLevelBox.setBackgroundColor(getResources().getColor(android.R.color.holo_green_dark));
                break;
            case "orange":
                riskLevelBox.setBackgroundColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "red":
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
