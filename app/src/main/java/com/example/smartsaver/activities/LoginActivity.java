package com.example.smartsaver.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.smartsaver.R;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailInput, passwordInput;
    private Button loginButton;
    private TextView registerRedirect, registerClickable;
    private CheckBox rememberCheckbox;

    private final String BASE_URL = "http://10.0.2.2:3000";
    private final String PREF_NAME = "LoginPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        registerRedirect = findViewById(R.id.registerRedirect);
        registerClickable = findViewById(R.id.registerClickable);
        rememberCheckbox = findViewById(R.id.rememberCheckbox);

        // SharedPreferences ile kaydedilen email varsa yükle
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String savedEmail = preferences.getString("email", "");
        boolean isRemembered = preferences.getBoolean("remember", false);
        if (isRemembered) {
            emailInput.setText(savedEmail);
            rememberCheckbox.setChecked(true);
        }

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show();
                return;
            }

            validateLogin(email, password);
        });

        // Hem üst yazıya hem "Register" yazısına tıklanabilirlik EKLENDİ


        registerClickable.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });
    }
    @Override
    public void onBackPressed() {
        finishAffinity();
        super.onBackPressed();
    }

    private void validateLogin(String email, String password) {
        String url = BASE_URL + "/login";

        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);

        JSONObject jsonBody = new JSONObject(params);

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, jsonBody,
                response -> {
                    try {
                        int userId = response.getInt("id");
                        String userEmail = response.getString("email");

                        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
                        if (rememberCheckbox.isChecked()) {
                            editor.putString("email", userEmail);
                            editor.putBoolean("remember", true);
                        } else {
                            editor.clear();
                        }
                        editor.apply();

                        Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(this, DashboardActivity.class);
                        intent.putExtra("user_id", userId);
                        intent.putExtra("user_email", userEmail);
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Login parse error", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    error.printStackTrace();
                    Toast.makeText(this, "Login failed: " + error.toString(), Toast.LENGTH_LONG).show();
                }
        );

        Volley.newRequestQueue(this).add(request);
    }
}
