 package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "phone_verification")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class PhoneVerification {

      private static final int CODE_TTL_MINUTES = 3;
      private static final int MAX_ATTEMPTS = 5;

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "phone", nullable = false)
      private String phone;

      @Column(name = "code", nullable = false)
      private String code;

      @Column(name = "purpose", nullable = false)
      private String purpose;     // SIGNUP / FIND_ID / FIND_PASSWORD

      @Column(name = "expires_at", nullable = false)
      private LocalDateTime expiresAt;

      @Column(name = "verified_at")
      private LocalDateTime verifiedAt;

      @Column(name = "attempts", nullable = false)
      private int attempts;

      @Column(name = "created_at", nullable = false)
      private LocalDateTime createdAt;

      @Builder
      public PhoneVerification(String phone, String code, String purpose) {
          this.phone = phone;
          this.code = code;
          this.purpose = purpose;
          LocalDateTime now = LocalDateTime.now();
          this.createdAt = now;
          this.expiresAt = now.plusMinutes(CODE_TTL_MINUTES);
          this.attempts = 0;
      }

      public boolean isExpired() {
          return LocalDateTime.now().isAfter(expiresAt);
      }

      public boolean isVerified() {
          return verifiedAt != null;
      }

      public boolean isMaxAttemptsReached() {
          return attempts >= MAX_ATTEMPTS;
      }

      public void incrementAttempts() {
          this.attempts++;
      }

      public void markAsVerified() {
          this.verifiedAt = LocalDateTime.now();
      }

      public void invalidate() {
          // 새 코드 발급 시 기존 미검증 코드 무효화하는 용도
          this.expiresAt = LocalDateTime.now().minusSeconds(1);
      }
  }
