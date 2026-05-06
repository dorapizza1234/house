package com.example.house.service;                                                                                                                                                                                                              import com.example.house.domain.FamilyMember;
  import com.example.house.domain.FamilyPlant;
  import com.example.house.domain.Member;
  import com.example.house.domain.PlantWateringLog;
  import com.example.house.dto.MemberContributionDto;
  import com.example.house.dto.PlantPlantRequest;
  import com.example.house.dto.PlantResponse;
  import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.FamilyPlantRepository;
  import com.example.house.repository.MemberRepository;
  import com.example.house.repository.PlantWateringLogRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  import java.time.LocalDate;
  import java.time.LocalDateTime;
  import java.util.ArrayList;
  import java.util.List;
  import java.util.Map;
  import java.util.stream.Collectors;

  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  public class PlantService {

      private static final int PLANT_COST = 30;
      private static final int WATER_COST = 30;
      private static final int REVIVE_COST = 50;

      private final FamilyPlantRepository familyPlantRepository;
      private final PlantWateringLogRepository wateringLogRepository;
      private final FamilyMemberRepository familyMemberRepository;
      private final MemberRepository memberRepository;

      @Transactional
      public PlantResponse plantPlant(Long memberId, PlantPlantRequest request) {
          FamilyMember member = findFamilyMember(memberId);
          Long familyId = member.getFamilyId();

          if (familyPlantRepository.existsByFamilyId(familyId)) {
              throw new IllegalArgumentException("이미 가족 식물이 있습니다");
          }
          if (member.getPoints() < PLANT_COST) {
              throw new IllegalArgumentException("포인트가 부족합니다 (필요: " + PLANT_COST + ")");
          }

          FamilyPlant plant = FamilyPlant.builder()
                  .familyId(familyId)
                  .name(request.name())
                  .plantedByMemberId(memberId)
                  .build();
          familyPlantRepository.save(plant);

          member.addPoints(-PLANT_COST);

          return toResponse(plant);
      }

      @Transactional
      public PlantResponse waterPlant(Long memberId) {
          FamilyMember member = findFamilyMember(memberId);
          FamilyPlant plant = familyPlantRepository.findByFamilyId(member.getFamilyId())
                  .orElseThrow(() -> new IllegalArgumentException("가족 식물이 없습니다. 먼저 심어주세요"));

          if (plant.isDead()) {
              throw new IllegalArgumentException("죽은 식물입니다. 부활부터 시켜주세요");
          }

          // 하루 1회 제한 (식물당)
          LocalDate today = LocalDate.now();
          long todayCount = wateringLogRepository.countByFamilyPlantIdAndWateredAtBetween(
                  plant.getId(), today.atStartOfDay(), today.plusDays(1).atStartOfDay());
          if (todayCount >= 1) {
              throw new IllegalArgumentException("오늘은 이미 물을 줬습니다");
          }

          if (member.getPoints() < WATER_COST) {
              throw new IllegalArgumentException("포인트가 부족합니다 (필요: " + WATER_COST + ")");
          }

          plant.water();      // last_watered_at 갱신 + WILTED → ALIVE
          member.addPoints(-WATER_COST);

          wateringLogRepository.save(PlantWateringLog.builder()
                  .familyPlantId(plant.getId())
                  .memberId(memberId)
                  .build());

          return toResponse(plant);
      }

      @Transactional
      public PlantResponse revivePlant(Long memberId) {
          FamilyMember member = findFamilyMember(memberId);
          FamilyPlant plant = familyPlantRepository.findByFamilyId(member.getFamilyId())
                  .orElseThrow(() -> new IllegalArgumentException("가족 식물이 없습니다"));

          if (!plant.isDead()) {
              throw new IllegalArgumentException("죽지 않은 식물은 부활시킬 수 없습니다");
          }
          if (member.getPoints() < REVIVE_COST) {
              throw new IllegalArgumentException("포인트가 부족합니다 (필요: " + REVIVE_COST + ")");
          }

          plant.revive();
          member.addPoints(-REVIVE_COST);

          return toResponse(plant);
      }

      public PlantResponse getMyFamilyPlant(Long memberId) {
          FamilyMember member = findFamilyMember(memberId);
          FamilyPlant plant = familyPlantRepository.findByFamilyId(member.getFamilyId())
                  .orElseThrow(() -> new IllegalArgumentException("가족 식물이 없습니다"));
          return toResponse(plant);
      }

      // === 내부 헬퍼 ===

      private FamilyMember findFamilyMember(Long memberId) {
          return familyMemberRepository.findByMemberId(memberId)
                  .stream().findFirst()
                  .orElseThrow(() -> new IllegalArgumentException("가족이 없습니다"));
      }

      private PlantResponse toResponse(FamilyPlant plant) {
          // 심은 사람 닉네임
          String plantedByNickname = memberRepository.findById(plant.getPlantedByMemberId())
                  .map(Member::getNickname)
                  .orElse("(알수없음)");

          // 가족 멤버별 물주기 카운트 (가시화)
          List<FamilyMember> familyMembers = familyMemberRepository.findByFamilyId(plant.getFamilyId());
          List<Long> memberIds = familyMembers.stream()
                  .map(FamilyMember::getMemberId)
                  .toList();

          Map<Long, String> nicknameMap = memberRepository.findAllById(memberIds).stream()
                  .collect(Collectors.toMap(Member::getId, Member::getNickname));

          List<MemberContributionDto> contributions = new ArrayList<>();
          for (Long mId : memberIds) {
              long count = wateringLogRepository.countByFamilyPlantIdAndMemberId(plant.getId(), mId);
              contributions.add(new MemberContributionDto(
                      mId, nicknameMap.getOrDefault(mId, "(알수없음)"), count));
          }

          return new PlantResponse(
                  plant.getId(),
                  plant.getFamilyId(),
                  plant.getName(),
                  plant.getState(),
                  plant.getPlantedByMemberId(),
                  plantedByNickname,
                  plant.getPlantedAt(),
                  plant.getLastWateredAt(),
                  plant.getStateChangedAt(),
                  plant.getHappiness(),
                  contributions);
      }
  }