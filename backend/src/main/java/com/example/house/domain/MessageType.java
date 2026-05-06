package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.AccessLevel;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "message_type")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class MessageType {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(nullable = false, unique = true, length = 30)
      private String code;

      @Column(name = "name_ko", nullable = false, length = 20)
      private String nameKo;

      @Column(name = "created_at", nullable = false)
      private LocalDateTime createdAt;

      @PrePersist
      protected void onCreate() {
          if (createdAt == null) createdAt = LocalDateTime.now();
      }

      @Builder
      private MessageType(String code, String nameKo) {
          this.code = code;
          this.nameKo = nameKo;
      }
  }