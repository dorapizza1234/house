package com.example.house.service;

  import com.example.house.domain.PhoneVerification;
  import com.example.house.repository.MemberRepository;
  import com.example.house.repository.PhoneVerificationRepository;
  import com.example.house.security.JwtUtil;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.security.SecureRandom;
  import java.time.LocalDateTime;

  @Service
  @RequiredArgsConstructor
  public class PhoneVerificationService {

      private static final int RATE_LIMIT_SECONDS = 60;
      private final SecureRandom random = new SecureRandom();

      private final PhoneVerificationRepository verificationRepository;
      private final MemberRepository memberRepository;
      private final SmsService smsService;
      private final JwtUtil jwtUtil;

      /** 코드 발송 */
      @Transactional
      public void sendCode(String phone, String purpose) {
          // 1. purpose별 사전 검증
          validatePurpose(phone, purpose);

          // 2. Rate limit (1분 내 재발송 차단)
          LocalDateTime since = LocalDateTime.now().minusSeconds(RATE_LIMIT_SECONDS);
          if (verificationRepository.existsByPhoneAndCreatedAtAfter(phone, since)) {
              throw new IllegalArgumentException("잠시 후 다시 시도해 주세요");
          }

          // 3. 같은 phone+purpose의 기존 미검증 코드 만료 처리
          verificationRepository.invalidateActiveCodes(phone, purpose, LocalDateTime.now());

          // 4. 6자리 코드 생성
          String code = String.format("%06d", random.nextInt(1_000_000));

          // 5. DB 저장
          PhoneVerification verification = PhoneVerification.builder()
                  .phone(phone)
                  .code(code)
                  .purpose(purpose)
                  .build();
          verificationRepository.save(verification);

          // 6. SMS 발송
          smsService.sendVerificationCode(phone, code);
      }

      /** 코드 검증 → 임시 토큰 발급 */
      @Transactional
      public String verifyCode(String phone, String code, String purpose) {
          PhoneVerification verification = verificationRepository
                  .findTopByPhoneAndPurposeOrderByCreatedAtDesc(phone, purpose)
                  .orElseThrow(() -> new IllegalArgumentException("인증번호 발송 이력이 없습니다"));

          if (verification.isVerified()) {
              throw new IllegalArgumentException("이미 사용된 인증번호입니다");
          }
          if (verification.isExpired()) {
              throw new IllegalArgumentException("만료된 인증번호입니다");
          }
          if (verification.isMaxAttemptsReached()) {
              throw new IllegalArgumentException("시도 횟수를 초과했습니다. 새 인증번호를 받아주세요");
          }

          verification.incrementAttempts();

          if (!verification.getCode().equals(code)) {
              throw new IllegalArgumentException("인증번호가 일치하지 않습니다");
          }

          verification.markAsVerified();

          // 검증 성공 → 10분 유효 임시 토큰 발급 (signup/findId에서 검증 증명용)
          return jwtUtil.generatePhoneVerificationToken(phone, purpose);
      }

      private void validatePurpose(String phone, String purpose) {
          switch (purpose) {
              case "SIGNUP" -> {
                  if (memberRepository.existsByPhone(phone)) {
                      throw new IllegalArgumentException("이미 가입된 번호입니다");
                  }
              }
              case "FIND_ID" -> {
                  if (!memberRepository.existsByPhone(phone)) {
                      throw new IllegalArgumentException("가입된 번호가 아닙니다");
                  }
              }
              default -> throw new IllegalArgumentException("유효하지 않은 요청입니다");
          }
      }
  }