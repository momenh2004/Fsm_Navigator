package com.fsm.navigator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fsm.navigator.auth.AuthService;
import com.fsm.navigator.auth.TokenManager;

/**
 * RegisterActivity.java – Contrôleur de la page d'inscription
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText    etEmail, etPassword, etConfirmPassword;
    private Button      btnRegister;
    private TextView    tvError, tvBackToLogin;
    private ProgressBar progressRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initViews();
        setupListeners();
    }

    private void initViews() {
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister       = findViewById(R.id.btnRegister);
        tvError           = findViewById(R.id.tvError);
        tvBackToLogin     = findViewById(R.id.tvLogin);
        progressRegister  = findViewById(R.id.progressRegister);
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(v -> attemptRegister());
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirm  = etConfirmPassword.getText().toString().trim();

        if (email.isEmpty()) {
            showError("Veuillez entrer votre email"); return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Adresse email invalide"); return;
        }
        if (password.isEmpty()) {
            showError("Veuillez entrer un mot de passe"); return;
        }
        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères"); return;
        }
        if (!password.equals(confirm)) {
            showError("Les mots de passe ne correspondent pas"); return;
        }

        setLoading(true);

        AuthService.register(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String token, String userEmail, String role) {
                TokenManager.saveToken(RegisterActivity.this, token);
                TokenManager.saveEmail(RegisterActivity.this, userEmail);
                TokenManager.saveRole(RegisterActivity.this, role);
                setLoading(false);
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showError(message);
            }
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        btnRegister.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressRegister.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);
    }
}