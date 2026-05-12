package com.example.house.service;

  import com.example.house.repository.MemberRepository;
  import com.example.house.security.JwtUtil;
  import lombok.RequiredArgsConstructor;
  import org.springframework.data.redis.core.StringRedisTemplate;
  import org.springframework.stereotype.Service;

  import java.security.SecureRandom;
  import java.time.Duration;

  @Service
  @RequiredArgsConstructor
  public class PhoneVerificationService {

      private static final Duration CODE_TTL = Duration.ofMinutes(3);
      private static final Duration COOLDOWN_TTL = Duration.ofSeconds(60);
      private static final int MAX_ATTEMPTS = 5;

      private final SecureRandom random = new SecureRandom();
      private final StringRedisTemplate redis;
      private final MemberRepository memberRepository;
      private final SmsService smsService;
      private final JwtUtil jwtUtil;

      /** 코드 발송 */
      public void sendCode(String phone, String purpose) {
          // 1. purpose별 사전 검증
          validatePurpose(phone, purpose);

          // 2. Rate limit (1분 내 재발송 차단) — SETNX + EX (atomic)
          Boolean firstTry = redis.opsForValue()
                  .setIfAbsent(cooldownKey(phone), "1", COOLDOWN_TTL);
          if (Boolean.FALSE.equals(firstTry)) {
              throw new IllegalArgumentException("잠시 후 다시 시도해 주세요");
          }

          // 3. 6자리 코드 생성
          String code = String.format("%06d", random.nextInt(1_000_000));

          // 4. Redis 저장 (기존 코드는 자동 덮어씀, TTL 3분)
          redis.opsForValue().set(codeKey(phone, purpose), code, CODE_TTL);
          // 새 코드 발급 → 시도 카운터 초기화
          redis.delete(attemptsKey(phone, purpose));

          // 5. SMS 발송
          smsService.sendVerificationCode(phone, code);
      }

      /** 코드 검증 → 임시 토큰 발급 */
      public String verifyCode(String phone, String code, String purpose) {
          String storedCode = redis.opsForValue().get(codeKey(phone, purpose));
          if (storedCode == null) {
              throw new IllegalArgumentException("인증번호가 만료되었거나 발송 이력이 없습니다");
          }

          // 시도 횟수 atomic INCR
          Long attempts = redis.opsForValue().increment(attemptsKey(phone, purpose));
          // 첫 시도일 때만 TTL 부여 (코드와 동일하게 3분)
          if (attempts != null && attempts == 1L) {
              redis.expire(attemptsKey(phone, purpose), CODE_TTL);
          }
          if (attempts != null && attempts > MAX_ATTEMPTS) {
              throw new IllegalArgumentException("시도 횟수를 초과했습니다. 새 인증번호를 받아주세요");
          }

          if (!storedCode.equals(code)) {
              throw new IllegalArgumentException("인증번호가 일치하지 않습니다");
          }

          // 검증 성공 → 키 정리 (재사용 방지)
          redis.delete(codeKey(phone, purpose));
          redis.delete(attemptsKey(phone, purpose));

          // 10분 유효 임시 토큰 발급
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

      private String codeKey(String phone, String purpose) {
          return "phone:verify:" + purpose + ":" + phone;
      }

      private String attemptsKey(String phone, String purpose) {
          return "phone:verify:attempts:" + purpose + ":" + phone;
      }

      private String cooldownKey(String phone) {
          return "phone:cooldown:" + phone;
      }
  }