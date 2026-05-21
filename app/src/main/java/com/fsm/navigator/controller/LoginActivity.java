package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.util.Patterns;
import android.widget.TextView;
import android.text.method.HideReturnsTransformationMethod;

import androidx.appcompat.app.AppCompatActivity;

import com.fsm.navigator.auth.AuthService;
import com.fsm.navigator.auth.PmrDialogHelper;
import com.fsm.navigator.auth.PmrManager;
import com.fsm.navigator.auth.TokenManager;
import com.fsm.navigator.auth.TtsManager;
import androidx.appcompat.widget.SwitchCompat;

//Handles User Login and navigation to the main app

public class LoginActivity extends AppCompatActivity {

    // Vues
    private EditText   etEmail, etPassword;
    private Button     btnLogin;
    private TextView   tvError, tvRegister, tvGuest;
    private ProgressBar progressLogin;
    private ImageButton btnTogglePassword;
    private boolean passwordVisible = false;
    private SwitchCompat switchPmr;

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
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnLogin         = findViewById(R.id.btnLogin);
        tvError          = findViewById(R.id.tvError);
        tvRegister       = findViewById(R.id.tvRegister);
        tvGuest          = findViewById(R.id.tvGuest);
        progressLogin    = findViewById(R.id.progressLogin);
        btnTogglePassword= findViewById(R.id.btnTogglePassword);
        switchPmr = findViewById(R.id.switchPmr);
    }
    private void setupListeners() {
        // Login
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            etPassword.setTransformationMethod(passwordVisible
                ? HideReturnsTransformationMethod.getInstance()
                : PasswordTransformationMethod.getInstance());
            etPassword.setSelection(etPassword.getText().length());
        });

        // Créer un compte → RegisterActivity
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Visiteur → MainActivity (avec dialog PMR si switch activé)
        tvGuest.setOnClickListener(v -> {
            if (switchPmr.isChecked()) {
                TtsManager.init(this);
                PmrManager.setEnabled(true);
                PmrDialogHelper.showProfileDialog(this, this::goToMain);
            } else {
                PmrManager.reset();
                goToMain();
            }
        });

        // PMR switch state is read at login time — no immediate side effect needed here.
    }

    private void attemptLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Vérifier les champs
        if (email.isEmpty()) showError("Veuillez entrer votre email");
        else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) showError("Adresse email invalide");
        else if (password.isEmpty()) showError("Veuillez entrer votre mot de passe");

        if (tvError.getVisibility() == View.VISIBLE) {
            return;
        }

        // Afficher le chargement
        setLoading(true);

        // Appel API
        AuthService.login(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(String token, String userEmail, String role) {
                // Sauvegarder le token et les infos
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
}
