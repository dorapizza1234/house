package com.example.house.service;
                                                                                                                          import com.example.house.domain.FamilyMember;
  import com.example.house.domain.FamilyPlant;
  import com.example.house.domain.Member;
  import com.example.house.domain.PlantWateringLog;
  import com.example.house.dto.PlantPlantRequest;
  import com.example.house.dto.PlantResponse;
  import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.FamilyPlantRepository;
  import com.example.house.repository.MemberRepository;
  import com.example.house.repository.PlantWateringLogRepository;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.test.util.ReflectionTestUtils;

  import java.time.LocalDate;
  import java.time.LocalDateTime;
  import java.util.Collections;
  import java.util.List;
  import java.util.Optional;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.ArgumentMatchers.anyLong;
  import static org.mockito.ArgumentMatchers.eq;
  import static org.mockito.BDDMockito.given;

  @ExtendWith(MockitoExtension.class)
  class PlantServiceTest {

      @Mock FamilyPlantRepository familyPlantRepository;
      @Mock PlantWateringLogRepository wateringLogRepository;
      @Mock FamilyMemberRepository familyMemberRepository;
      @Mock MemberRepository memberRepository;

      @InjectMocks PlantService plantService;

      private static final Long MEMBER_ID = 1L;
      private static final Long FAMILY_ID = 10L;
      private static final Long PLANT_ID = 100L;

      // ============ plantPlant ============

      @Test
      void plantPlant_성공() {
          FamilyMember member = setupFamilyMember(50);  // 50pt 보유
          given(familyPlantRepository.existsByFamilyId(FAMILY_ID)).willReturn(false);
          given(familyPlantRepository.save(any(FamilyPlant.class))).willAnswer(inv -> {
              FamilyPlant p = inv.getArgument(0);
              ReflectionTestUtils.setField(p, "id", PLANT_ID);
              ReflectionTestUtils.setField(p, "plantedAt", LocalDateTime.now());
              ReflectionTestUtils.setField(p, "lastWateredAt", LocalDateTime.now());
              ReflectionTestUtils.setField(p, "stateChangedAt", LocalDateTime.now());
              ReflectionTestUtils.setField(p, "state", "ALIVE");
              ReflectionTestUtils.setField(p, "happiness", 0);
              return p;
          });
          setupResponseMocks();

          PlantResponse result = plantService.plantPlant(MEMBER_ID, new PlantPlantRequest("초록이"));

          assertThat(result.name()).isEqualTo("초록이");
          assertThat(result.state()).isEqualTo("ALIVE");
          assertThat(member.getPoints()).isEqualTo(20);  // 50 - 30
      }

      @Test
      void plantPlant_이미식물있음_예외() {
          setupFamilyMember(50);
          given(familyPlantRepository.existsByFamilyId(FAMILY_ID)).willReturn(true);

          assertThatThrownBy(() -> plantService.plantPlant(MEMBER_ID, new PlantPlantRequest("초록이")))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("이미 가족 식물이 있습니다");
      }

      @Test
      void plantPlant_포인트부족_예외() {
          setupFamilyMember(20);  // 30 미만
          given(familyPlantRepository.existsByFamilyId(FAMILY_ID)).willReturn(false);

          assertThatThrownBy(() -> plantService.plantPlant(MEMBER_ID, new PlantPlantRequest("초록이")))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("포인트가 부족합니다");
      }

      // ============ waterPlant ============

      @Test
      void waterPlant_성공() {
          FamilyMember member = setupFamilyMember(50);
          FamilyPlant plant = setupPlant("ALIVE");
          given(wateringLogRepository.countByFamilyPlantIdAndWateredAtBetween(
                  eq(PLANT_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                  .willReturn(0L);
          given(wateringLogRepository.save(any(PlantWateringLog.class))).willAnswer(inv -> inv.getArgument(0));
          setupResponseMocks();

          PlantResponse result = plantService.waterPlant(MEMBER_ID);

          assertThat(result.state()).isEqualTo("ALIVE");
          assertThat(member.getPoints()).isEqualTo(20);  // 50 - 30
      }

      @Test
      void waterPlant_DEAD식물_예외() {
          setupFamilyMember(50);
          setupPlant("DEAD");

          assertThatThrownBy(() -> plantService.waterPlant(MEMBER_ID))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("죽은 식물");
      }

      @Test
      void waterPlant_오늘이미줬음_예외() {
          setupFamilyMember(50);
          setupPlant("ALIVE");
          given(wateringLogRepository.countByFamilyPlantIdAndWateredAtBetween(
                  eq(PLANT_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                  .willReturn(1L);  // 이미 1번

          assertThatThrownBy(() -> plantService.waterPlant(MEMBER_ID))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("오늘은 이미 물을 줬습니다");
      }

      @Test
      void waterPlant_포인트부족_예외() {
          setupFamilyMember(20);
          setupPlant("ALIVE");
          given(wateringLogRepository.countByFamilyPlantIdAndWateredAtBetween(
                  eq(PLANT_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                  .willReturn(0L);

          assertThatThrownBy(() -> plantService.waterPlant(MEMBER_ID))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("포인트가 부족합니다");
      }

      @Test
      void waterPlant_WILTED를_물주면_ALIVE() {
          FamilyMember member = setupFamilyMember(50);
          FamilyPlant plant = setupPlant("WILTED");
          given(wateringLogRepository.countByFamilyPlantIdAndWateredAtBetween(
                  eq(PLANT_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                  .willReturn(0L);
          given(wateringLogRepository.save(any(PlantWateringLog.class))).willAnswer(inv -> inv.getArgument(0));
          setupResponseMocks();

          PlantResponse result = plantService.waterPlant(MEMBER_ID);

          assertThat(result.state()).isEqualTo("ALIVE");  // WILTED → ALIVE 자동 회복
      }

      // ============ revivePlant ============

      @Test
      void revivePlant_성공() {
          FamilyMember member = setupFamilyMember(70);
          setupPlant("DEAD");
          setupResponseMocks();

          PlantResponse result = plantService.revivePlant(MEMBER_ID);

          assertThat(result.state()).isEqualTo("ALIVE");
          assertThat(member.getPoints()).isEqualTo(20);  // 70 - 50
      }

      @Test
      void revivePlant_ALIVE식물_예외() {
          setupFamilyMember(70);
          setupPlant("ALIVE");

          assertThatThrownBy(() -> plantService.revivePlant(MEMBER_ID))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("죽지 않은");
      }

      @Test
      void revivePlant_포인트부족_예외() {
          setupFamilyMember(40);  // 50 미만
          setupPlant("DEAD");

          assertThatThrownBy(() -> plantService.revivePlant(MEMBER_ID))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("포인트가 부족합니다");
      }

      // ============ getMyFamilyPlant ============

      @Test
      void getMyFamilyPlant_성공() {
          setupFamilyMember(50);
          setupPlant("ALIVE");
          setupResponseMocks();

          PlantResponse result = plantService.getMyFamilyPlant(MEMBER_ID);

          assertThat(result.id()).isEqualTo(PLANT_ID);
          assertThat(result.contributions()).isNotNull();
      }

      @Test
      void getMyFamilyPlant_식물없음_예외() {
          setupFamilyMember(50);
          given(familyPlantRepository.findByFamilyId(FAMILY_ID)).willReturn(Optional.empty());

          assertThatThrownBy(() -> plantService.getMyFamilyPlant(MEMBER_ID))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("가족 식물이 없습니다");
      }

      // ============ 헬퍼 ============

      private FamilyMember setupFamilyMember(int points) {
          FamilyMember member = FamilyMember.builder()
                  .familyId(FAMILY_ID).memberId(MEMBER_ID).role("OWNER").build();
          ReflectionTestUtils.setField(member, "id", 200L);
          ReflectionTestUtils.setField(member, "points", points);
          given(familyMemberRepository.findByMemberId(MEMBER_ID)).willReturn(List.of(member));
          return member;
      }

      private FamilyPlant setupPlant(String state) {
          FamilyPlant plant = FamilyPlant.builder()
                  .familyId(FAMILY_ID).name("초록이").plantedByMemberId(MEMBER_ID).build();
          ReflectionTestUtils.setField(plant, "id", PLANT_ID);
          ReflectionTestUtils.setField(plant, "state", state);
          ReflectionTestUtils.setField(plant, "plantedAt", LocalDateTime.now().minusDays(5));
          ReflectionTestUtils.setField(plant, "lastWateredAt", LocalDateTime.now().minusHours(10));
          ReflectionTestUtils.setField(plant, "stateChangedAt", LocalDateTime.now().minusHours(10));
          ReflectionTestUtils.setField(plant, "happiness", 0);
          given(familyPlantRepository.findByFamilyId(FAMILY_ID)).willReturn(Optional.of(plant));
          return plant;
      }

      private void setupResponseMocks() {
          // 심은 사람 닉네임
          Member alice = Member.builder()
                  .email("a@t.com").passwordHash("h").nickname("alice")
                  .birthDate(LocalDate.of(1990, 1, 1)).build();
          ReflectionTestUtils.setField(alice, "id", MEMBER_ID);
          given(memberRepository.findById(MEMBER_ID)).willReturn(Optional.of(alice));

          // 가족 멤버 리스트 (1명만 있는 가족)
          FamilyMember fm = FamilyMember.builder()
                  .familyId(FAMILY_ID).memberId(MEMBER_ID).role("OWNER").build();
          ReflectionTestUtils.setField(fm, "id", 200L);
          given(familyMemberRepository.findByFamilyId(FAMILY_ID)).willReturn(List.of(fm));

          // 닉네임 매핑
          given(memberRepository.findAllById(any(Iterable.class))).willReturn(List.of(alice));

          // 멤버별 물주기 카운트
          given(wateringLogRepository.countByFamilyPlantIdAndMemberId(eq(PLANT_ID), anyLong()))
                  .willReturn(3L);
      }
  }
