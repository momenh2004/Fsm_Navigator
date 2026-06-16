// =====================================================
// 1. OtpService.java — Génère et valide le code OTP
// =====================================================
package com.fsm.navigator.backend.service;

import org.springframework.stereotype.Service;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class OtpService {

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final Map<String, OtpEntry> otpStore = new HashMap<>();

    private static class OtpEntry {
        String code;
        LocalDateTime expiry;
        OtpEntry(String code) {
            this.code   = code;
            this.expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        }
    }

    // Générer un OTP à 6 chiffres
    public String generateOtp(String email) {
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));
        otpStore.put(email, new OtpEntry(code));
        return code;
    }

    // Valider l'OTP
    public boolean validateOtp(String email, String code) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null) return false;
        if (LocalDateTime.now().isAfter(entry.expiry)) {
            otpStore.remove(email);
            return false;
        }
        if (entry.code.equals(code)) {
            otpStore.remove(email); // usage unique
            return true;
        }
        return false;
    }
}