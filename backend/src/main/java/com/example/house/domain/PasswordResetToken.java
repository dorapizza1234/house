  package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "password_reset_token")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class PasswordResetToken {

      private static final int TOKEN_TTL_HOURS = 1;

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "member_id", nullable = false)
      private Long memberId;

      @Column(name = "token", nullable = false, unique = true)
      private String token;

      @Column(name = "expires_at", nullable = false)
      private LocalDateTime expiresAt;

      @Column(name = "used_at")
      private LocalDateTime usedAt;

      @Column(name = "created_at", nullable = false)
      private LocalDateTime createdAt;

      @Builder
      public PasswordResetToken(Long memberId, String token) {
          this.memberId = memberId;
          this.token = token;
          LocalDateTime now = LocalDateTime.now();
          this.createdAt = now;
          this.expiresAt = now.plusHours(TOKEN_TTL_HOURS);
      }

      public boolean isExpired() {
          return LocalDateTime.now().isAfter(expiresAt);
      }

      public boolean isUsed() {
          return usedAt != null;
      }

      public void markAsUsed() {
          this.usedAt = LocalDateTime.now();
      }
  }