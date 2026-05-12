package com.fsm.navigator.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fsm.navigator.R;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class AdminLoginActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:8080/api/admin";

    private LinearLayout layoutStep1, layoutStep2;
    private EditText     etAdminEmail, etOtpCode;
    private Button       btnRequestOtp, btnVerifyOtp;
    private TextView     tvAdminError, tvResendOtp;
    private ProgressBar  progressAdmin;
    private ImageButton  btnBack;

    private String adminEmail = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);
        initViews();
        setupListeners();
    }

    private void initViews() {
        layoutStep1   = findViewById(R.id.layoutStep1);
        layoutStep2   = findViewById(R.id.layoutStep2);
        etAdminEmail  = findViewById(R.id.etAdminEmail);
        etOtpCode     = findViewById(R.id.etOtpCode);
        btnRequestOtp = findViewById(R.id.btnRequestOtp);
        btnVerifyOtp  = findViewById(R.id.btnVerifyOtp);
        tvAdminError  = findViewById(R.id.tvAdminError);
        tvResendOtp   = findViewById(R.id.tvResendOtp);
        progressAdmin = findViewById(R.id.progressAdmin);
        btnBack       = findViewById(R.id.btnBack);
    }

    private void setupListeners() {

        // ✅ Bouton retour
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

        btnRequestOtp.setOnClickListener(v -> {
            String email = etAdminEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Entrez votre email", Toast.LENGTH_SHORT).show();
                return;
            }
            adminEmail = email;
            requestOtp(email);
        });

        btnVerifyOtp.setOnClickListener(v -> {
            String code = etOtpCode.getText().toString().trim();
            if (code.length() != 6) {
                showError("Le code doit contenir 6 chiffres");
                return;
            }
            verifyOtp(adminEmail, code);
        });

        if (tvResendOtp != null)
            tvResendOtp.setOnClickListener(v -> requestOtp(adminEmail));
    }

    // ===== ÉTAPE 1 =====
    private void requestOtp(String email) {
        setLoading(true);
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);

                URL url = new URL(BASE_URL + "/request-otp");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int status = conn.getResponseCode();
                // APRÈS ✅
                runOnUiThread(() -> {
                    setLoading(false);
                    if (status == 200) {
                        layoutStep1.setVisibility(View.GONE);
                        layoutStep2.setVisibility(View.VISIBLE);
                        Toast.makeText(getApplicationContext(), "Code envoyé à " + email, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Email non autorisé.", Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getApplicationContext(), "Impossible de contacter le serveur.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    // ===== ÉTAPE 2 =====
    private void verifyOtp(String email, String code) {
        setLoading(true);
        hideError();

        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("code", code);

                URL url = new URL(BASE_URL + "/verify-otp");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(10000);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                int status = conn.getResponseCode();
                String token = "";
                if (status == 200) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();
                    token = new JSONObject(sb.toString()).getString("token");
                }

                final String finalToken  = token;
                final int    finalStatus = status;

                runOnUiThread(() -> {
                    setLoading(false);
                    if (finalStatus == 200) {
                        Intent intent = new Intent(this, AdminDashboardActivity.class);
                        intent.putExtra("ADMIN_TOKEN", finalToken);
                        intent.putExtra("ADMIN_EMAIL", email);
                        startActivity(intent);
                        finish();
                    } else {
                        showError("Code invalide ou expiré. Réessayez.");
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(getApplicationContext(), "Impossible de contacter le serveur.", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void setLoading(boolean loading) {
        progressAdmin.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRequestOtp.setEnabled(!loading);
        btnVerifyOtp.setEnabled(!loading);
    }

    private void showError(String msg) {
        if (tvAdminError != null) {
            tvAdminError.setText(msg);
            tvAdminError.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (tvAdminError != null) tvAdminError.setVisibility(View.GONE);
    }
}