package com.example.house.repository;

  import com.example.house.domain.PlantWateringLog;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.time.LocalDateTime;

  public interface PlantWateringLogRepository extends JpaRepository<PlantWateringLog, Long> {

      // 하루 1회 제한 검증용
      long countByFamilyPlantIdAndWateredAtBetween(
              Long familyPlantId, LocalDateTime start, LocalDateTime end);

      // 멤버별 누적 카운트 (가시화 — 공동 책임 압박)
      long countByFamilyPlantIdAndMemberId(Long familyPlantId, Long memberId);
  }