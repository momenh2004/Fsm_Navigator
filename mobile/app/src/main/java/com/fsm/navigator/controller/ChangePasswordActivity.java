package com.fsm.navigator.controller;

import com.fsm.navigator.R;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.fsm.navigator.auth.AuthService;
import com.fsm.navigator.auth.TokenManager;

/**
 * ChangePasswordActivity.java
 *
 * Permet à l'utilisateur connecté de modifier son mot de passe.
 * Appelle POST /api/auth/change-password avec le token JWT.
 *
 * Couche MVC : Contrôleur
 */
public class ChangePasswordActivity extends AppCompatActivity {

    private EditText    etOldPassword, etNewPassword, etConfirmNewPassword;
    private Button      btnConfirm;
    private TextView    tvError;
    private ProgressBar progressChange;
    private ImageButton btnBack, btnToggleOld, btnToggleNew;

    private boolean oldVisible = false;
    private boolean newVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);
        initViews();
        setupListeners();
    }

    // =========================================================
    private void initViews() {
        etOldPassword        = findViewById(R.id.etOldPassword);
        etNewPassword        = findViewById(R.id.etNewPassword);
        etConfirmNewPassword = findViewById(R.id.etConfirmPassword);
        btnConfirm           = findViewById(R.id.btnChangePassword);
        tvError              = findViewById(R.id.tvError);
        progressChange       = findViewById(R.id.progressChange);
        btnBack              = findViewById(R.id.btnBack);
    }

    // =========================================================
    private void setupListeners() {

        // Retour
        btnBack.setOnClickListener(v -> finish());

        // Afficher/masquer ancien mdp
        if (btnToggleOld != null) btnToggleOld.setOnClickListener(v -> {
            oldVisible = !oldVisible;
            etOldPassword.setTransformationMethod(
                    oldVisible ? null : new PasswordTransformationMethod());
            etOldPassword.setSelection(etOldPassword.getText().length());
        });

        // Afficher/masquer nouveau mdp
        if (btnToggleNew != null) btnToggleNew.setOnClickListener(v -> {
            newVisible = !newVisible;
            etNewPassword.setTransformationMethod(
                    newVisible ? null : new PasswordTransformationMethod());
            etConfirmNewPassword.setTransformationMethod(
                    newVisible ? null : new PasswordTransformationMethod());
            etNewPassword.setSelection(etNewPassword.getText().length());
        });

        // Confirmer le changement
        btnConfirm.setOnClickListener(v -> attemptChangePassword());
    }

    // =========================================================
    private void attemptChangePassword() {
        String oldPwd     = etOldPassword.getText().toString().trim();
        String newPwd     = etNewPassword.getText().toString().trim();
        String confirmPwd = etConfirmNewPassword.getText().toString().trim();

        // Validations
        if (oldPwd.isEmpty()) {
            showError("Veuillez entrer votre ancien mot de passe"); return;
        }
        if (newPwd.isEmpty()) {
            showError("Veuillez entrer un nouveau mot de passe"); return;
        }
        if (newPwd.length() < 6) {
            showError("Le nouveau mot de passe doit contenir au moins 6 caractères"); return;
        }
        if (!newPwd.equals(confirmPwd)) {
            showError("Les mots de passe ne correspondent pas"); return;
        }
        if (oldPwd.equals(newPwd)) {
            showError("Le nouveau mot de passe doit être différent de l'ancien"); return;
        }

        setLoading(true);

        // Appel API
        String token = TokenManager.getToken(this);
        AuthService.changePassword(token, oldPwd, newPwd, new AuthService.SimpleCallback() {
            @Override
            public void onSuccess(String message) {
                setLoading(false);
                Toast.makeText(ChangePasswordActivity.this,
                        "Mot de passe modifié avec succès !", Toast.LENGTH_LONG).show();
                finish();
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
        btnConfirm.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressChange.setVisibility(loading ? View.VISIBLE : View.GONE);
        tvError.setVisibility(View.GONE);
    }
}
