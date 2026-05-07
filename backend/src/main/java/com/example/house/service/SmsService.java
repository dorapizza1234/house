 package com.example.house.service;

  import lombok.extern.slf4j.Slf4j;
  import net.nurigo.sdk.NurigoApp;
  import net.nurigo.sdk.message.model.Message;
  import net.nurigo.sdk.message.service.DefaultMessageService;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Service;

  import jakarta.annotation.PostConstruct;

  @Slf4j
  @Service
  public class SmsService {

      @Value("${coolsms.api-key}")
      private String apiKey;

      @Value("${coolsms.api-secret}")
      private String apiSecret;

      @Value("${coolsms.from-number}")
      private String fromNumber;

      private DefaultMessageService messageService;

      @PostConstruct
      public void init() {
          this.messageService = NurigoApp.INSTANCE.initialize(
                  apiKey, apiSecret, "https://api.coolsms.co.kr"
          );
      }

      public void sendVerificationCode(String toPhone, String code) {
          Message message = new Message();
          message.setFrom(fromNumber);
          message.setTo(toPhone);
          message.setText("[우리집] 인증번호 [" + code + "] (3분 내 입력)");

          try {
              messageService.send(message);
              log.info("SMS 발송 성공 to={}", toPhone);
          } catch (Exception e) {
              log.error("SMS 발송 실패 to={}", toPhone, e);
              throw new RuntimeException("SMS 발송 실패", e);
          }
      }
  }