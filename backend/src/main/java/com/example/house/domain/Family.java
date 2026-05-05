package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "family")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class Family {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "id")
      private Long id;

      @Column(name = "name", nullable = false)
      private String name;

      @Column(name = "creator_id", nullable = false)
      private Long creatorId;

      @Column(name = "created_at")
      private LocalDateTime createdAt;

      @Builder
      public Family(String name, Long creatorId) {
          this.name = name;
          this.creatorId = creatorId;
          this.createdAt = LocalDateTime.now();
      }
  }