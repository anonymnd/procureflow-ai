package com.project.app.identity.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class IdentityEmailService {
    private final ObjectProvider<JavaMailSender> mailSenders;
    private final String publicBaseUrl;
    private final String fromAddress;

    public IdentityEmailService(ObjectProvider<JavaMailSender> mailSenders,
            @Value("${PROCUREFLOW_PUBLIC_BASE_URL:http://localhost:8080}") String publicBaseUrl,
            @Value("${PROCUREFLOW_EMAIL_FROM:no-reply@localhost}") String fromAddress) {
        this.mailSenders = mailSenders;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
        this.fromAddress = fromAddress;
    }
    public void sendVerification(String email, String token) { send(email, "Verify your ProcureFlow account", "/?verify=", token); }
    public void sendInvitation(String email, String token) { send(email, "ProcureFlow organization invitation", "/?invite=", token); }
    private void send(String email, String subject, String path, String token) {
        JavaMailSender sender = mailSenders.getIfAvailable();
        if (sender == null || fromAddress.isBlank()) throw IdentityException.unavailable();
        String link = publicBaseUrl + path + URLEncoder.encode(token, StandardCharsets.UTF_8);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject(subject);
        message.setText("Complete this one-time action using the link below:\n\n" + link + "\n\nIf you did not request this, ignore this email.");
        try {
            sender.send(message);
        } catch (RuntimeException deliveryFailure) {
            // Never return provider diagnostics or bearer-token content to the caller.
            throw IdentityException.unavailable();
        }
    }
}
