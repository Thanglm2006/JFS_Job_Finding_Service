package com.example.JFS_Job_Finding_Service.Services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

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
    public CompletableFuture<Void> sendResetPasswordCode(String to, String code) throws MessagingException {
        String subject = "🔐 JFS: Mã xác nhận để đặt lại mật khẩu";

        String htmlContent = """
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 0; background: linear-gradient(135deg, #4C51BF10, #4CAF5010);">
                <!-- Header -->
                <div style="background: linear-gradient(135deg, #4C51BF, #4CAF50); padding: 40px 20px; text-align: center; border-radius: 20px 20px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px; font-weight: 600; text-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        JFS Job Finding Service
                    </h1>
                    <div style="width: 60px; height: 4px; background: rgba(255,255,255,0.3); margin: 15px auto; border-radius: 2px;"></div>
                </div>

                <!-- Content -->
                <div style="background: white; padding: 40px 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <div style="width: 80px; height: 80px; background: linear-gradient(135deg, #4C51BF, #4CAF50); border-radius: 50%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 25px rgba(76, 81, 191, 0.3);">
                            <span style="color: white; font-size: 30px;">🔐</span>
                        </div>
                        <h2 style="color: #333; margin: 0 0 10px 0; font-size: 24px; font-weight: 600;">Đặt lại mật khẩu</h2>
                    </div>

                    <p style="color: #555; font-size: 16px; line-height: 1.6; margin-bottom: 30px; text-align: center;">
                        Vui lòng sử dụng mã xác nhận sau để hoàn tất việc đặt lại mật khẩu:
                    </p>

                    <!-- Verification Code -->
                    <div style="text-align: center; margin: 40px 0;">
                        <div style="display: inline-block; background: linear-gradient(135deg, #4CAF50, #45a049); color: white; font-size: 32px; font-weight: bold; padding: 20px 40px; border-radius: 15px; letter-spacing: 8px; box-shadow: 0 8px 25px rgba(76, 175, 80, 0.4); border: 3px solid rgba(255,255,255,0.2);">
                            {{Code}}
                        </div>
                    </div>

                    <div style="background: linear-gradient(135deg, #f8f9ff, #f0f8ff); padding: 25px; border-radius: 15px; margin-top: 30px; border-left: 4px solid #4C51BF;">
                        <p style="color: #666; font-size: 14px; line-height: 1.6; margin: 0;">
                            <strong>⏰ Lưu ý:</strong> Mã này sẽ hết hạn sau 5 phút. Nếu bạn không gửi yêu cầu, vui lòng bỏ qua email này.
                        </p>
                    </div>
                </div>

                <!-- Footer -->
                <div style="background: #f8f9fa; padding: 30px 20px; text-align: center; border-radius: 0 0 20px 20px;">
                    <p style="color: #888; font-size: 12px; margin: 0; line-height: 1.5;">
                        © 2025 JFS Job Finding Service. Cảm ơn bạn đã tin tưởng chúng tôi! 💝
                    </p>
                    <div style="margin-top: 15px;">
                        <span style="color: #ddd; margin: 0 10px;">•</span>
                        <a href="#" style="color: #888; text-decoration: none; font-size: 12px;">Chính sách bảo mật</a>
                        <span style="color: #ddd; margin: 0 10px;">•</span>
                        <a href="#" style="color: #888; text-decoration: none; font-size: 12px;">Điều khoản sử dụng</a>
                    </div>
                </div>
            </div>
            """.replace("{{Code}}", code);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        try {
            mailSender.send(message);
        } catch (MailException e) {
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }
    @Async
    public void sendVerificationEmailHTML(String to, String code) throws MessagingException, MailException {

        String subject = "🔐 JFS: Mã xác nhận để xác minh email";

        String htmlContent = """
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 0; background: linear-gradient(135deg, #4C51BF10, #4CAF5010);">
                        <div style="background: linear-gradient(135deg, #4C51BF, #4CAF50); padding: 40px 20px; text-align: center; border-radius: 20px 20px 0 0;">
                          <h1 style="color: white; margin: 0; font-size: 28px; font-weight: 600; text-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                            JFS Job Finding Service
                          </h1>
                          <div style="width: 60px; height: 4px; background: rgba(255,255,255,0.3); margin: 15px auto; border-radius: 2px;"></div>
                        </div>
                
                        <div style="background: white; padding: 40px 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                          <div style="text-align: center; margin-bottom: 30px;">
                            <div style="width: 80px; height: 80px; background: linear-gradient(135deg, #4C51BF, #4CAF50); border-radius: 50%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 25px rgba(76, 81, 191, 0.3);">
                              <span style="color: white; font-size: 30px;">🔐</span>
                            </div>
                            <h2 style="color: #333; margin: 0 0 10px 0; font-size: 24px; font-weight: 600;">Xác minh email</h2>
                          </div>
                
                          <p style="color: #555; font-size: 16px; line-height: 1.6; margin-bottom: 30px; text-align: center;">
                            Vui lòng sử dụng mã xác nhận sau để hoàn tất việc xác minh email:
                          </p>
                
                          <div style="text-align: center; margin: 40px 0;">
                            <div style="display: inline-block; background: linear-gradient(135deg, #4CAF50, #45a049); color: white; font-size: 32px; font-weight: bold; padding: 20px 40px; border-radius: 15px; letter-spacing: 8px; box-shadow: 0 8px 25px rgba(76, 175, 80, 0.4); border: 3px solid rgba(255,255,255,0.2);">
                              {{Code}}
                            </div>
                          </div>
                
                          <div style="background: linear-gradient(135deg, #f8f9ff, #f0f8ff); padding: 25px; border-radius: 15px; margin-top: 30px; border-left: 4px solid #4C51BF;">
                            <p style="color: #666; font-size: 14px; line-height: 1.6; margin: 0;">
                              <strong>⏰ Lưu ý:</strong> Mã này sẽ hết hạn sau 5 phút. Nếu bạn không gửi yêu cầu, vui lòng bỏ qua email này.
                            </p>
                          </div>
                        </div>
                
                        <div style="background: #f8f9fa; padding: 30px 20px; text-align: center; border-radius: 0 0 20px 20px;">
                          <p style="color: #888; font-size: 12px; margin: 0; line-height: 1.5;">
                            © 2025 JFS Job Finding Service. Cảm ơn bạn đã tin tương chúng tôi! 💝
                          </p>
                        </div>
                      </div>
            """.replace("{{Code}}", code);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
    @Async
    public void sendInterviewInvitation(String to, String name, String jobTitle, String interviewDate, String roomLink) throws MessagingException {
        String subject = "📅 JFS: Thư mời phỏng vấn - " + jobTitle;

        String htmlContent = """
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: 0 auto; padding: 0; background: linear-gradient(135deg, #4C51BF10, #4CAF5010);">
                <div style="background: linear-gradient(135deg, #4C51BF, #4CAF50); padding: 40px 20px; text-align: center; border-radius: 20px 20px 0 0;">
                    <h1 style="color: white; margin: 0; font-size: 28px; font-weight: 600; text-shadow: 0 2px 4px rgba(0,0,0,0.1);">
                        JFS Job Finding Service
                    </h1>
                    <div style="width: 60px; height: 4px; background: rgba(255,255,255,0.3); margin: 15px auto; border-radius: 2px;"></div>
                </div>

                <div style="background: white; padding: 40px 30px; box-shadow: 0 4px 20px rgba(0,0,0,0.1);">
                    <div style="text-align: center; margin-bottom: 30px;">
                        <div style="width: 80px; height: 80px; background: linear-gradient(135deg, #4C51BF, #4CAF50); border-radius: 50%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center; box-shadow: 0 8px 25px rgba(76, 81, 191, 0.3);">
                            <span style="color: white; font-size: 35px;">📅</span>
                        </div>
                        <h2 style="color: #333; margin: 0 0 10px 0; font-size: 24px; font-weight: 600;">Thư Mời Phỏng Vấn</h2>
                        <p style="color: #666; font-size: 16px; margin: 10px 0;">Xin chào <strong>{{Name}}</strong>,</p>
                    </div>

                    <p style="color: #555; font-size: 15px; line-height: 1.6; text-align: center; margin-bottom: 30px;">
                        Chúng tôi rất ấn tượng với hồ sơ của bạn và trân trọng mời bạn tham gia buổi phỏng vấn cho vị trí <strong>{{JobTitle}}</strong>.
                    </p>

                    <div style="background-color: #f8f9fa; border-radius: 15px; padding: 25px; margin-bottom: 30px; border: 1px solid #eee;">
                        <table style="width: 100%; border-collapse: collapse;">
                            <tr>
                                <td style="padding: 10px 0; color: #666; font-size: 14px; width: 40%;"><strong>Vị trí:</strong></td>
                                <td style="padding: 10px 0; color: #333; font-size: 14px; font-weight: 600;">{{JobTitle}}</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #666; font-size: 14px; border-top: 1px solid #eee;"><strong>Thời gian:</strong></td>
                                <td style="padding: 10px 0; color: #333; font-size: 14px; font-weight: 600; border-top: 1px solid #eee;">{{Date}}</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #666; font-size: 14px; border-top: 1px solid #eee;"><strong>Hình thức:</strong></td>
                                <td style="padding: 10px 0; color: #333; font-size: 14px; font-weight: 600; border-top: 1px solid #eee;">Phỏng vấn trực tuyến</td>
                            </tr>
                        </table>
                    </div>

                    <div style="text-align: center; margin: 30px 0;">
                        <a href="{{Link}}" style="display: inline-block; background: linear-gradient(135deg, #4CAF50, #45a049); color: white; text-decoration: none; font-size: 16px; font-weight: bold; padding: 15px 35px; border-radius: 30px; box-shadow: 0 5px 15px rgba(76, 175, 80, 0.4); transition: transform 0.2s;">
                            Tham Gia Phòng Phỏng Vấn
                        </a>
                        <p style="margin-top: 15px; font-size: 13px; color: #888;">
                            Hoặc copy link: <a href="{{Link}}" style="color: #4C51BF;">{{Link}}</a>
                        </p>
                    </div>

                    <div style="background: linear-gradient(135deg, #fff3cd, #fff8e1); padding: 20px; border-radius: 12px; margin-top: 20px; border-left: 4px solid #ffc107;">
                        <p style="color: #856404; font-size: 14px; line-height: 1.5; margin: 0;">
                            <strong>💡 Lưu ý:</strong> Vui lòng tham gia trước 5 phút để kiểm tra thiết bị và đường truyền.
                        </p>
                    </div>
                </div>

                <div style="background: #f8f9fa; padding: 30px 20px; text-align: center; border-radius: 0 0 20px 20px;">
                    <p style="color: #888; font-size: 12px; margin: 0; line-height: 1.5;">
                        © 2025 JFS Job Finding Service.<br>
                        Email này được gửi tự động, vui lòng không trả lời.
                    </p>
                </div>
            </div>
            """
                .replace("{{Name}}", name)
                .replace("{{JobTitle}}", jobTitle)
                .replace("{{Date}}", interviewDate)
                .replace("{{Link}}", roomLink);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}