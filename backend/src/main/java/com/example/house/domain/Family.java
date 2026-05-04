package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "그룹")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class Family {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "가족id")
      private Long id;

      @Column(name = "집안이름", nullable = false)
      private String name;

      @Column(name = "생성자_id", nullable = false)
      private Long creatorId;

      @Column(name = "생성일시")
      private LocalDateTime createdAt;

      @Builder
      public Family(String name, Long creatorId) {
          this.name = name;
          this.creatorId = creatorId;
          this.createdAt = LocalDateTime.now();
      }
  }