package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.AccessLevel;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "family_plant")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class FamilyPlant {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "family_id", nullable = false, unique = true)
      private Long familyId;

      @Column(nullable = false, length = 50)
      private String name;

      @Column(nullable = false, length = 20)
      private String state;     // ALIVE / WILTED / DEAD

      @Column(name = "planted_by_member_id", nullable = false)
      private Long plantedByMemberId;

      @Column(name = "planted_at", nullable = false)
      private LocalDateTime plantedAt;

      @Column(name = "last_watered_at", nullable = false)
      private LocalDateTime lastWateredAt;

      @Column(name = "state_changed_at", nullable = false)
      private LocalDateTime stateChangedAt;

      @Column(nullable = false)
      private Integer happiness;

      @Version
      private Long version;       // 낙관적 락

      @Column(name = "created_at", nullable = false)
      private LocalDateTime createdAt;

      @PrePersist
      protected void onCreate() {
          LocalDateTime now = LocalDateTime.now();
          if (createdAt == null) createdAt = now;
          if (plantedAt == null) plantedAt = now;
          if (lastWateredAt == null) lastWateredAt = now;
          if (stateChangedAt == null) stateChangedAt = now;
          if (state == null) state = "ALIVE";
          if (happiness == null) happiness = 0;
      }

      @Builder
      private FamilyPlant(Long familyId, String name, Long plantedByMemberId) {
          this.familyId = familyId;
          this.name = name;
          this.plantedByMemberId = plantedByMemberId;
      }

      // === 도메인 메서드 ===

      public void water() {
          this.lastWateredAt = LocalDateTime.now();
          if (!"ALIVE".equals(this.state)) {
              this.state = "ALIVE";
              this.stateChangedAt = LocalDateTime.now();
          }
      }

      public void wilt() {
          this.state = "WILTED";
          this.stateChangedAt = LocalDateTime.now();
      }

      public void die() {
          this.state = "DEAD";
          this.stateChangedAt = LocalDateTime.now();
      }

      public void revive() {
          this.state = "ALIVE";
          this.lastWateredAt = LocalDateTime.now();
          this.stateChangedAt = LocalDateTime.now();
      }

      public boolean isDead() {
          return "DEAD".equals(this.state);
      }

      public boolean isAlive() {
          return "ALIVE".equals(this.state);
      }
  }
