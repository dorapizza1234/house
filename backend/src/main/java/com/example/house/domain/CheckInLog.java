package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;
                                                                                                                          import java.time.LocalDateTime;
                                                                                                                          @Entity
  @Table(name = "check_in_log")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class CheckInLog {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "id")
      private Long id;

      @Column(name = "member_id", nullable = false)
      private Long memberId;

      @Column(name = "family_id")
      private Long familyId;

      @Column(name = "status", nullable = false, length = 10)
      private String status;

      @Column(name = "checked_at", nullable = false)
      private LocalDateTime checkedAt;

      @Builder
      public CheckInLog(Long memberId, Long familyId, String status, LocalDateTime checkedAt) {
          this.memberId = memberId;
          this.familyId = familyId;
          this.status = status;
          this.checkedAt = checkedAt;
      }
  }