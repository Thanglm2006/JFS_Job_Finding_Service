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
    public void sendResetPasswordCode(String to, String code) throws MessagingException {
        String subject = "🔐 JFS: Mã xác nhận để đặt lại mật khẩu";

        String htmlContent = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; background: #f9f9f9; border-radius: 10px;">
                <h1 style="text-align: center; color: #4C51BF;">JFS Job Finding Service</h1>
                <h2 style="color: #333;">Đặt lại mật khẩu</h2>
                <p>Người dùng thân mến,</p>
                <p>Vui lòng sử dụng mã xác nhận sau để hoàn tất việc đặt lại mật khẩu:</p>
                <div style="font-size: 24px; font-weight: bold; color: #fff; background-color: #4CAF50; padding: 10px 20px; border-radius: 8px; display: inline-block;">
                    %s
                </div>
                <p style="margin-top: 20px;">Mã này sẽ hết hạn sau 5 phút. Nếu bạn không gửi yêu cầu, vui lòng bỏ qua email này.</p>
                <p style="color: #888; font-size: 12px; margin-top: 30px;">Cảm ơn bạn!</p>
            </div>
            """.formatted(code);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    @Async
    public void sendVerificationEmailHTML(String to, String code) {
        try {
            String subject = "🔐 JFS: Mã xác nhận để xác minh email";

            String htmlContent = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; background: #f9f9f9; border-radius: 10px;">
                    <h1 style="text-align: center; color: #4C51BF;">JFS Job Finding Service</h1>
                    <h2 style="color: #333;">Xác minh email</h2>
                    <p>Người dùng thân mến,</p>
                    <p>Vui lòng sử dụng mã xác nhận sau để hoàn tất việc xác minh email:</p>
                    <div style="font-size: 24px; font-weight: bold; color: #fff; background-color: #4CAF50; padding: 10px 20px; border-radius: 8px; display: inline-block;">
                        %s
                    </div>
                    <p style="margin-top: 20px;">Mã này sẽ hết hạn sau 5 phút. Nếu bạn không gửi yêu cầu, vui lòng bỏ qua email này.</p>
                    <p style="color: #888; font-size: 12px; margin-top: 30px;">Cảm ơn bạn!</p>
                </div>
                """.formatted(code);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}