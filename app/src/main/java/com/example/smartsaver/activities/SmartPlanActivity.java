package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.smartsaver.R;

public class SmartPlanActivity extends AppCompatActivity {

    private EditText inputAmount, inputDuration, inputRisk;
    private Spinner goalSpinner;
    private ImageButton btnContinue;

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_plan);

        inputAmount = findViewById(R.id.inputAmount);
        inputDuration = findViewById(R.id.inputDuration);
        inputRisk = findViewById(R.id.inputRisk);
        goalSpinner = findViewById(R.id.goalSpinner);
        btnContinue = findViewById(R.id.btnContinue);

        userEmail = getIntent().getStringExtra("user_email");

        // Spinner içerikleri
        String[] goals = {"Select a goal", "Buy a house", "Car", "Vacation", "Emergency Fund"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, goals);
        goalSpinner.setAdapter(adapter);

        btnContinue.setOnClickListener(v -> handleContinue());
    }

    private void handleContinue() {
        String amountStr = inputAmount.getText().toString().trim();
        String durationStr = inputDuration.getText().toString().trim();
        String riskStr = inputRisk.getText().toString().trim();
        String goal = goalSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(durationStr) || TextUtils.isEmpty(riskStr) || goal.equals("Select a goal")) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);

        Intent intent = new Intent(this, SuggestionResultActivity.class);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("amount", amount);
        intent.putExtra("duration", durationStr);
        intent.putExtra("risk", riskStr);
        intent.putExtra("goal", goal);
        startActivity(intent);
    }
}
