package com.example.house.scheduler;

  import com.example.house.domain.FamilyPlant;
  import com.example.house.repository.FamilyPlantRepository;
  import lombok.RequiredArgsConstructor;
  import lombok.extern.slf4j.Slf4j;
  import org.springframework.scheduling.annotation.Scheduled;
  import org.springframework.stereotype.Component;
  import org.springframework.transaction.annotation.Transactional;

  import java.time.LocalDateTime;
  import java.util.List;

  @Slf4j
  @Component
  @RequiredArgsConstructor
  public class PlantStateScheduler {

      private static final long WILTED_HOURS = 24;
      private static final long DEAD_HOURS = 72;
      private static final long DEAD_AUTO_DELETE_DAYS = 7;

      private final FamilyPlantRepository familyPlantRepository;

      /**
       * 1시간마다 식물 상태 자동 전이
       *  - ALIVE + 24h 안 줌 → WILTED
       *  - WILTED + 72h 안 줌 → DEAD
       *  - DEAD + 7일 경과 → 자동 소멸 (삭제)
       *
       *  주의: MVP는 단일 인스턴스 배포 가정. 다중 인스턴스 시 ShedLock 필요.
       */
      @Scheduled(fixedRate = 3600000)  // 1시간 = 3,600,000ms
      @Transactional
      public void updatePlantStates() {
          LocalDateTime now = LocalDateTime.now();

          // 1) ALIVE → WILTED
          LocalDateTime wiltThreshold = now.minusHours(WILTED_HOURS);
          List<FamilyPlant> toWilt = familyPlantRepository
                  .findByStateAndLastWateredAtBefore("ALIVE", wiltThreshold);
          for (FamilyPlant plant : toWilt) {
              plant.wilt();
          }
          if (!toWilt.isEmpty()) {
              log.info("ALIVE → WILTED 전이: {}개", toWilt.size());
          }

          // 2) WILTED → DEAD
          LocalDateTime deadThreshold = now.minusHours(DEAD_HOURS);
          List<FamilyPlant> toDie = familyPlantRepository
                  .findByStateAndLastWateredAtBefore("WILTED", deadThreshold);
          for (FamilyPlant plant : toDie) {
              plant.die();
          }
          if (!toDie.isEmpty()) {
              log.info("WILTED → DEAD 전이: {}개", toDie.size());
          }

          // 3) DEAD 7일 경과 → 자동 삭제
          LocalDateTime autoDeleteThreshold = now.minusDays(DEAD_AUTO_DELETE_DAYS);
          List<FamilyPlant> toDelete = familyPlantRepository
                  .findByStateAndStateChangedAtBefore("DEAD", autoDeleteThreshold);
          if (!toDelete.isEmpty()) {
              familyPlantRepository.deleteAll(toDelete);
              log.info("DEAD 7일 경과 자동 삭제: {}개", toDelete.size());
          }
      }
  }
