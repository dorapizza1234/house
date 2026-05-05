package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "family_invite")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class FamilyInvite {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "id")
      private Long id;

      @Column(name = "family_id", nullable = false)
      private Long familyId;

      @Column(name = "token", nullable = false, length = 500)
      private String token;

      @Column(name = "inviter_id", nullable = false)
      private Long inviterId;

      @Column(name = "expires_at", nullable = false)
      private LocalDateTime expiresAt;

      @Column(name = "used_at")
      private LocalDateTime usedAt;

      @Column(name = "used_by_id")
      private Long usedById;

      @Builder
      public FamilyInvite(Long familyId, String token, Long inviterId) {
          this.familyId = familyId;
          this.token = token;
          this.inviterId = inviterId;
          this.expiresAt = LocalDateTime.now().plusHours(24);
      }

      public boolean isExpired() {
          return LocalDateTime.now().isAfter(expiresAt);
      }

      public boolean isUsed() {
          return usedAt != null;
      }

      public void markAsUsed(Long userId) {
          this.usedAt = LocalDateTime.now();
          this.usedById = userId;
      }
  }