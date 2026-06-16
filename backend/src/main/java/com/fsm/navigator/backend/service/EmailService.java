package com.fsm.navigator.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public void sendOtp(String toEmail, String otpCode) {
        if (mailSender == null) {
            System.out.println("==============================");
            System.out.println("  CODE OTP ADMIN : " + otpCode);
            System.out.println("==============================");
            return;
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(toEmail);
        msg.setSubject("FSM Navigator — Code Admin");
        msg.setText("Votre code : " + otpCode + "\n\nValable 10 minutes.");
        mailSender.send(msg);
    }
}