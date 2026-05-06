package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "family_member")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class FamilyMember {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "id")
      private Long id;

      @Column(name = "family_id", nullable = false)
      private Long familyId;

      @Column(name = "member_id", nullable = false)
      private Long memberId;

      @Column(name = "role", nullable = false)
      private String role;

      @Column(name = "points", nullable = false)
      private int points;

      @Column(name = "joined_at")
      private LocalDateTime joinedAt;

      @Builder
      public FamilyMember(Long familyId, Long memberId, String role) {
          this.familyId = familyId;
          this.memberId = memberId;
          this.role = role;
          this.points = 0;
          this.joinedAt = LocalDateTime.now();
      }
      
      public void addPoints(int points) {
          this.points += points;
      }
  }