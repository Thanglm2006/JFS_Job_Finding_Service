package com.example.JFS_Job_Finding_Service.Services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;
    @Value("${spring.mail.username}")
    private String fromEmail;
    @Async
    public void sendSimpleEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }


    @Async
    public void sendVerificationHtmlEmail(String to, String code) throws MessagingException {

        String subject = "🔐 Verify your email address";

        String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; background: #f9f9f9; border-radius: 10px;">
                    <h2 style="color: #333;">Email Verification</h2>
                    <p>Dear user,</p>
                    <p>Use the following verification code to complete your sign-up process:</p>
                    <div style="font-size: 24px; font-weight: bold; color: #fff; background-color: #4CAF50; padding: 10px 20px; border-radius: 8px; display: inline-block;">
                        %s
                    </div>
                    <p style="margin-top: 20px;">This code will expire in 5 minutes. If you didn't request this, please ignore this email.</p>
                    <p style="color: #888; font-size: 12px; margin-top: 30px;">Thank you,<br>Your Company Team</p>
                </div>
                """.formatted(code);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true); // true = is HTML

        mailSender.send(message);
    }

}
