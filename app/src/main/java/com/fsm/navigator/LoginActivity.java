package com.fsm.navigator;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.fsm.navigator.admin.AdminLoginActivity;
import com.fsm.navigator.auth.AuthService;
import com.fsm.navigator.auth.TokenManager;
import androidx.appcompat.widget.SwitchCompat;
import com.fsm.navigator.auth.PmrManager;

/**
 * LoginActivity.java – Contrôleur de la page de connexion
 *
 * Flux :
 *  1. L'utilisateur saisit email + mot de passe
 *  2. AuthService appelle POST /api/auth/login
 *  3. En cas de succès → token sauvegardé → redirect MainActivity
 *  4. En cas d'erreur  → message affiché en rouge
 *  5. "Continuer sans compte" → redirect directement MainActivity
 */
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

        // Si déjà connecté → aller directement à MainActivity
        if (TokenManager.isLoggedIn(this)) {
            goToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        initViews();
        setupListeners();
    }

    // =========================================================
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

    // =========================================================
    private void setupListeners() {

        // Bouton connexion
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Afficher/masquer mot de passe
        btnTogglePassword.setOnClickListener(v -> {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                etPassword.setTransformationMethod(null);
            } else {
                etPassword.setTransformationMethod(new PasswordTransformationMethod());
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // Aller à RegisterActivity
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
        });

        // Continuer sans compte → MainActivity directement
        tvGuest.setOnClickListener(v -> goToMain());

        switchPmr.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PmrManager.setEnabled(isChecked);
        });
        TextView tvAdminAccess = findViewById(R.id.tvAdminAccess);
        tvAdminAccess.setOnClickListener(v ->
                startActivity(new Intent(this, AdminLoginActivity.class))
        );
    }

    // =========================================================
    private void attemptLogin() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validation locale
        if (email.isEmpty()) {
            showError("Veuillez entrer votre email");
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Adresse email invalide");
            return;
        }
        if (password.isEmpty()) {
            showError("Veuillez entrer votre mot de passe");
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
                PmrManager.setEnabled(switchPmr.isChecked());
                goToMain();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                showError(message);
            }
        });
    }

    // =========================================================
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