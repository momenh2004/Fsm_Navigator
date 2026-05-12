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

import java.util.Random;

/**
 * RegisterActivity.java – Inscription avec captcha mathématique
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText    etEmail, etPassword, etConfirmPassword, etCaptcha;
    private Button      btnRegister;
    private TextView    tvError, tvBackToLogin, tvCaptchaQuestion;
    private ProgressBar progressRegister;

    private int captchaAnswer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initViews();
        generateCaptcha();
        setupListeners();
    }

    private void initViews() {
        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        etCaptcha         = findViewById(R.id.etCaptcha);
        btnRegister       = findViewById(R.id.btnRegister);
        tvError           = findViewById(R.id.tvError);
        tvBackToLogin     = findViewById(R.id.tvLogin);
        tvCaptchaQuestion = findViewById(R.id.tvCaptchaQuestion);
        progressRegister  = findViewById(R.id.progressRegister);
    }

    // =========================================================
    // CAPTCHA MATHÉMATIQUE
    // =========================================================
    private void generateCaptcha() {
        Random rand = new Random();
        int a = rand.nextInt(10) + 1;  // 1–10
        int b = rand.nextInt(10) + 1;  // 1–10
        int op = rand.nextInt(3);       // 0=+, 1=-, 2=×

        String question;
        switch (op) {
            case 0:
                captchaAnswer = a + b;
                question = "Combien font  " + a + " + " + b + " ?";
                break;
            case 1:
                // s'assurer que la réponse est positive
                if (a < b) { int tmp = a; a = b; b = tmp; }
                captchaAnswer = a - b;
                question = "Combien font  " + a + " − " + b + " ?";
                break;
            default:
                captchaAnswer = a * b;
                question = "Combien font  " + a + " × " + b + " ?";
                break;
        }

        if (tvCaptchaQuestion != null)
            tvCaptchaQuestion.setText(question);
    }

    private boolean verifyCaptcha() {
        String input = etCaptcha != null ? etCaptcha.getText().toString().trim() : "";
        if (input.isEmpty()) {
            showError("Veuillez répondre à la question de vérification");
            return false;
        }
        try {
            int answer = Integer.parseInt(input);
            if (answer != captchaAnswer) {
                showError("Réponse incorrecte — veuillez réessayer");
                generateCaptcha();
                if (etCaptcha != null) etCaptcha.setText("");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showError("Veuillez entrer un nombre");
            return false;
        }
    }

    // =========================================================
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

        // ✅ Vérifier le captcha avant d'appeler l'API
        if (!verifyCaptcha()) return;

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