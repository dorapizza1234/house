package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.AccessLevel;
  import lombok.Builder;
  import lombok.Getter;
  import lombok.NoArgsConstructor;

  import java.time.LocalDateTime;

  @Entity
  @Table(name = "plant_watering_log")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class PlantWateringLog {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      private Long id;

      @Column(name = "family_plant_id", nullable = false)
      private Long familyPlantId;

      @Column(name = "member_id", nullable = false)
      private Long memberId;

      @Column(name = "watered_at", nullable = false)
      private LocalDateTime wateredAt;

      @PrePersist
      protected void onCreate() {
          if (wateredAt == null) wateredAt = LocalDateTime.now();
      }

      @Builder
      private PlantWateringLog(Long familyPlantId, Long memberId) {
          this.familyPlantId = familyPlantId;
          this.memberId = memberId;
      }
  }