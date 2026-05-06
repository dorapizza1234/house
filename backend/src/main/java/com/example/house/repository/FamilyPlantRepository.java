package com.example.house.repository;                                                                                 
  import com.example.house.domain.FamilyPlant;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.time.LocalDateTime;
  import java.util.List;
  import java.util.Optional;

  public interface FamilyPlantRepository extends JpaRepository<FamilyPlant, Long> {

      // 가족 식물 조회 (가족당 1개라 Optional)
      Optional<FamilyPlant> findByFamilyId(Long familyId);

      // 가족이 식물 가지고 있는지 확인 (심기 시 중복 방지)
      boolean existsByFamilyId(Long familyId);

      // 스케줄러용: 특정 상태 + 마지막 물준 시각 이전 식물들
      List<FamilyPlant> findByStateAndLastWateredAtBefore(String state, LocalDateTime threshold);

      // 스케줄러용: DEAD 상태 + 7일 자동 소멸 대상
      List<FamilyPlant> findByStateAndStateChangedAtBefore(String state, LocalDateTime threshold);
  }