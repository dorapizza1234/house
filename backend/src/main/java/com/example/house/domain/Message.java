 package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.AccessLevel;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "message")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class Message {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "family_id", nullable = false)
      private Long familyId;

      @Column(name = "sender_id", nullable = false)
      private Long senderId;

      @Column(name = "receiver_id", nullable = false)
      private Long receiverId;

      @Column(name = "message_type_id")
      private Long messageTypeId;

      @Column(nullable = false, columnDefinition = "TEXT")
      private String content;

      @Column(name = "base_points", nullable = false)
      private Integer basePoints;

      @Column(nullable = false)
      private Integer multiplier;

      @Column(name = "final_points", nullable = false)
      private Integer finalPoints;

      @Column(name = "event_reasons", length = 500)
      private String eventReasons;

      @Column(name = "created_at", nullable = false)
      private LocalDateTime createdAt;

      @PrePersist
      protected void onCreate() {
          if (createdAt == null) createdAt = LocalDateTime.now();
          if (multiplier == null) multiplier = 1;
      }

      @Builder
      private Message(Long familyId, Long senderId, Long receiverId,
                      Long messageTypeId, String content,
                      Integer basePoints, Integer multiplier, Integer finalPoints,
                      String eventReasons) {
          this.familyId = familyId;
          this.senderId = senderId;
          this.receiverId = receiverId;
          this.messageTypeId = messageTypeId;
          this.content = content;
          this.basePoints = basePoints;
          this.multiplier = multiplier;
          this.finalPoints = finalPoints;
          this.eventReasons = eventReasons;
      }
  }
