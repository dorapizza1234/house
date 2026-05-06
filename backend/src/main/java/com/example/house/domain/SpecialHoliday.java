package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.AccessLevel;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "special_holiday")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class SpecialHoliday {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(nullable = false, unique = true, length = 30)
      private String code;

      @Column(name = "name_ko", nullable = false, length = 30)
      private String nameKo;

      @Column(nullable = false)
      private Integer month;

      @Column(nullable = false)
      private Integer day;

      @Column(name = "created_at", nullable = false)
      private LocalDateTime createdAt;

      @PrePersist
      protected void onCreate() {
          if (createdAt == null) createdAt = LocalDateTime.now();
      }

      @Builder
      private SpecialHoliday(String code, String nameKo, Integer month, Integer day) {
          this.code = code;
          this.nameKo = nameKo;
          this.month = month;
          this.day = day;
      }
  }
