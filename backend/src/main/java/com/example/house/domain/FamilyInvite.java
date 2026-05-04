package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "가족초대")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class FamilyInvite {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "초대id")
      private Long id;

      @Column(name = "가족id", nullable = false)
      private Long familyId;

      @Column(name = "초대토큰", nullable = false, length = 500)
      private String token;

      @Column(name = "초대한사람_id", nullable = false)
      private Long inviterId;

      @Column(name = "만료시각", nullable = false)
      private LocalDateTime expiresAt;

      @Column(name = "사용시각")
      private LocalDateTime usedAt;

      @Column(name = "사용한사람_id")
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