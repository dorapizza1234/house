 package com.example.house.service;

  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.mail.javamail.JavaMailSender;
  import org.springframework.mail.javamail.MimeMessageHelper;
  import org.springframework.stereotype.Service;

  import jakarta.mail.MessagingException;
  import jakarta.mail.internet.MimeMessage;

  @Slf4j
  @Service
  @RequiredArgsConstructor
  public class EmailService {

      private final JavaMailSender mailSender;

      @Value("${app.base-url}")
      private String baseUrl;

      @Value("${spring.mail.username}")
      private String fromEmail;

      public void sendPasswordResetEmail(String toEmail, String token) {
          String resetLink = baseUrl + "/reset-password?token=" + token;

          String html = """
                  <div style="font-family: sans-serif; max-width: 480px; margin: 0 auto;">
                      <h2 style="color: #5E8560;">[우리집] 비밀번호 재설정</h2>
                      <p>아래 버튼을 눌러 새 비밀번호를 설정해 주세요.</p>
                      <p>이 링크는 <strong>1시간</strong> 동안만 유효합니다.</p>
                      <a href="%s" style="
                          display: inline-block;
                          padding: 12px 24px;
                          background: #7FA67E;
                          color: white;
                          border-radius: 8px;
                          text-decoration: none;
                          margin: 16px 0;
                      ">비밀번호 재설정</a>
                      <p style="color: #888; font-size: 12px;">
                          본인이 요청하지 않았다면 이 메일을 무시해 주세요.
                      </p>
                  </div>
                  """.formatted(resetLink);

          try {
              MimeMessage message = mailSender.createMimeMessage();
              MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
              helper.setFrom(fromEmail);
              helper.setTo(toEmail);
              helper.setSubject("[우리집] 비밀번호 재설정 안내");
              helper.setText(html, true);   // true = HTML

              mailSender.send(message);
              log.info("비밀번호 재설정 메일 발송 to={}", toEmail);
          } catch (MessagingException e) {
              log.error("메일 발송 실패 to={}", toEmail, e);
              throw new RuntimeException("메일 발송 실패", e);
          }
      }
  }
