package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "가족멤버")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class FamilyMember {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "가족멤버id")
      private Long id;

      @Column(name = "가족id", nullable = false)
      private Long familyId;

      @Column(name = "개인아이디", nullable = false)
      private Long memberId;

      @Column(name = "역할", nullable = false)
      private String role;

      @Column(name = "포인트", nullable = false)
      private int points;

      @Column(name = "가입일")
      private LocalDateTime joinedAt;

      @Builder
      public FamilyMember(Long familyId, Long memberId, String role) {
          this.familyId = familyId;
          this.memberId = memberId;
          this.role = role;
          this.points = 0;
          this.joinedAt = LocalDateTime.now();
      }
  }