package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fsm.navigator.auth.AuthService;
import com.fsm.navigator.auth.PmrDialogHelper;
import com.fsm.navigator.auth.PmrManager;
import com.fsm.navigator.auth.TokenManager;
import com.fsm.navigator.auth.TtsManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout emailLayout, pwLayout;
    private MaterialButton btnLogin;
    private TextView tvError;
    private ProgressBar progressLogin;
    private MaterialSwitch switchPmr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (TokenManager.isLoggedIn(this)) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
    }

    private void initViews() {
        emailLayout   = findViewById(R.id.emailLayout);
        pwLayout      = findViewById(R.id.pwLayout);
        etEmail       = findViewById(R.id.etEmail);
        etPassword    = findViewById(R.id.etPassword);
        btnLogin      = findViewById(R.id.btnLogin);
        tvError       = findViewById(R.id.tvError);
        progressLogin = findViewById(R.id.progressLogin);
        switchPmr     = findViewById(R.id.switchPmr);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        ((MaterialButton) findViewById(R.id.tvRegister)).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));

        ((MaterialButton) findViewById(R.id.tvGuest)).setOnClickListener(v -> {
            if (switchPmr.isChecked()) {
                TtsManager.init(this);
                PmrManager.setEnabled(true);
                PmrDialogHelper.showProfileDialog(this, this::goToMain);
            } else {
                PmrManager.reset();
                goToMain();
            }
        });
    }

    private void attemptLogin() {
        String email = text(etEmail);
        String password = text(etPassword);

        emailLayout.setError(null);
        pwLayout.setError(null);
        tvError.setVisibility(View.GONE);

        boolean ok = true;
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError("Adresse e-mail invalide");
            ok = false;
        }
        if (password.length() < 6) {
            pwLayout.setError("Au moins 6 caractères");
            ok = false;
        }
        if (!ok) return;

        setLoading(true);

        AuthService.login(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String token, String userEmail, String role) {
                TokenManager.saveToken(LoginActivity.this, token);
                TokenManager.saveEmail(LoginActivity.this, userEmail);
                TokenManager.saveRole(LoginActivity.this, role);
                setLoading(false);
                if (switchPmr.isChecked()) {
                    TtsManager.init(LoginActivity.this);
                    PmrDialogHelper.showProfileDialog(LoginActivity.this, LoginActivity.this::goToMain);
                } else {
                    PmrManager.reset();
                    goToMain();
                }
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    setLoading(false);
                    showError(message);
                });
            }
        });
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        btnLogin.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressLogin.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void goToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static String text(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}
