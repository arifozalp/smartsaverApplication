package com.example.smartsaver.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.smartsaver.R;

public class SmartPlanActivity extends AppCompatActivity {

    private EditText inputAmount;
    private RadioGroup durationGroup, riskGroup;
    private Spinner goalSpinner;
    private Button btnContinue;

    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_plan);

        inputAmount = findViewById(R.id.inputAmount);
        durationGroup = findViewById(R.id.durationGroup);
        riskGroup = findViewById(R.id.riskGroup);
        goalSpinner = findViewById(R.id.goalSpinner);
        btnContinue = findViewById(R.id.btnContinue);

        userEmail = getIntent().getStringExtra("user_email");

        // Spinner veri yüklemesi
        String[] goals = {"Select a goal", "Buy a house", "Car", "Vacation", "Emergency Fund"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, goals);
        goalSpinner.setAdapter(adapter);

        btnContinue.setOnClickListener(v -> handleContinue());
    }

    private void handleContinue() {
        String amountStr = inputAmount.getText().toString().trim();
        int selectedDurationId = durationGroup.getCheckedRadioButtonId();
        int selectedRiskId = riskGroup.getCheckedRadioButtonId();
        String goal = goalSpinner.getSelectedItem().toString();

        if (TextUtils.isEmpty(amountStr) || selectedDurationId == -1 || selectedRiskId == -1 || goal.equals("Select a goal")) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        String duration = ((RadioButton) findViewById(selectedDurationId)).getText().toString();
        String risk = ((RadioButton) findViewById(selectedRiskId)).getText().toString();

        // Verileri SuggestionResultActivity'ye gönder
        Intent intent = new Intent(this, SuggestionResultActivity.class);
        intent.putExtra("user_email", userEmail);
        intent.putExtra("amount", amount);
        intent.putExtra("duration", duration);
        intent.putExtra("risk", risk);
        intent.putExtra("goal", goal);
        startActivity(intent);
    }
}
