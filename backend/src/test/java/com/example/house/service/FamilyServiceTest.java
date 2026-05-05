package com.example.house.service;

  import com.example.house.domain.Family;
import com.example.house.domain.FamilyInvite;
import com.example.house.domain.FamilyMember;
  import com.example.house.dto.CreateFamilyRequest;
  import com.example.house.dto.CreateFamilyResponse;
import com.example.house.dto.CreateInviteResponse;
import com.example.house.repository.FamilyInviteRepository;
  import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.FamilyRepository;
import com.example.house.repository.MemberRepository;
import com.example.house.security.JwtUtil;
  import org.junit.jupiter.api.DisplayName;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.test.util.ReflectionTestUtils;

  import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.ArgumentMatchers.argThat;
  import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import com.example.house.dto.AcceptInviteRequest;
import com.example.house.dto.AcceptInviteResponse;
import com.example.house.domain.FamilyInvite;
import java.time.LocalDateTime;
import java.util.Optional;

import com.example.house.domain.FamilyMember;
import com.example.house.domain.Member;
import com.example.house.dto.DashboardMemberDto;
import java.time.LocalDate;
import java.util.List;
  @ExtendWith(MockitoExtension.class)
  class FamilyServiceTest {

      @Mock private FamilyRepository familyRepository;
      @Mock private FamilyMemberRepository familyMemberRepository;
      @Mock private FamilyInviteRepository familyInviteRepository;
      @Mock private JwtUtil jwtUtil;
      @Mock private MemberRepository memberRepository;
      @InjectMocks private FamilyService familyService;

      @Test
      @DisplayName("가족 생성 성공 - 가족 + 생성자를 OWNER로 동시 저장")
      void createFamily_성공() {
          // given
          CreateFamilyRequest request = new CreateFamilyRequest("집으로");
          Long creatorId = 1L;

          Family savedFamily = Family.builder()
                  .name("집으로")
                  .creatorId(creatorId)
                  .build();
          ReflectionTestUtils.setField(savedFamily, "id", 100L);

          given(familyRepository.save(any(Family.class))).willReturn(savedFamily);

          // when
          CreateFamilyResponse response = familyService.createFamily(request, creatorId);

          // then
          assertThat(response.familyId()).isEqualTo(100L);
          assertThat(response.name()).isEqualTo("집으로");

          verify(familyRepository).save(any(Family.class));
          verify(familyMemberRepository).save(argThat(fm ->
                  fm.getRole().equals("OWNER") &&
                  fm.getMemberId().equals(creatorId) &&
                  fm.getFamilyId().equals(100L)
          ));
      }
      

      @Test
      @DisplayName("초대 생성 성공 - 가족 멤버가 토큰 만들고 DB 저장")
      void createInvite_성공() {
          // given
          Long familyId = 100L;
          Long inviterId = 1L;
          String generatedToken = "fake.jwt.token";

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, inviterId))
                  .willReturn(true);
          given(jwtUtil.generateInviteToken(familyId)).willReturn(generatedToken);

          FamilyInvite savedInvite = FamilyInvite.builder()
                  .familyId(familyId)
                  .token(generatedToken)
                  .inviterId(inviterId)
                  .build();
          given(familyInviteRepository.save(any(FamilyInvite.class))).willReturn(savedInvite);

          // when
          CreateInviteResponse response = familyService.createInvite(familyId, inviterId);

          // then
          assertThat(response.token()).isEqualTo(generatedToken);
          assertThat(response.expiresAt()).isNotNull();
          verify(familyInviteRepository).save(any(FamilyInvite.class));
      }

      @Test
      @DisplayName("초대 생성 실패 - 가족 멤버 아니면 IllegalArgumentException")
      void createInvite_가족멤버아님_실패() {
          // given
          Long familyId = 100L;
          Long inviterId = 999L;

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, inviterId))
                  .willReturn(false);

          // when & then
          assertThatThrownBy(() -> familyService.createInvite(familyId, inviterId))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("가족 멤버만 초대할 수 있습니다");

          verify(jwtUtil, never()).generateInviteToken(any());
          verify(familyInviteRepository, never()).save(any(FamilyInvite.class));
      }
      
      
      @Test
      @DisplayName("초대 수락 성공 - 새 멤버 MEMBER로 추가 + 초대 사용 처리")
      void acceptInvite_성공() {
          // given
          String token = "valid.jwt.token";
          AcceptInviteRequest request = new AcceptInviteRequest(token);
          Long memberId = 2L;
          Long familyId = 100L;

          given(jwtUtil.getFamilyIdFromInviteToken(token)).willReturn(familyId);

          FamilyInvite invite = FamilyInvite.builder()
                  .familyId(familyId)
                  .token(token)
                  .inviterId(1L)
                  .build();
          given(familyInviteRepository.findByToken(token)).willReturn(Optional.of(invite));
          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, memberId)).willReturn(false);

          Family family = Family.builder()
                  .name("집으로")
                  .creatorId(1L)
                  .build();
          ReflectionTestUtils.setField(family, "id", familyId);
          given(familyRepository.findById(familyId)).willReturn(Optional.of(family));

          // when
          AcceptInviteResponse response = familyService.acceptInvite(request, memberId);

          // then
          assertThat(response.familyId()).isEqualTo(familyId);
          assertThat(response.familyName()).isEqualTo("집으로");

          verify(familyMemberRepository).save(argThat(fm ->
                  fm.getRole().equals("MEMBER") &&
                  fm.getMemberId().equals(memberId) &&
                  fm.getFamilyId().equals(familyId)
          ));

          assertThat(invite.isUsed()).isTrue();  // markAsUsed 호출되어 상태 변경됐는지
      }

      @Test
      @DisplayName("초대 수락 실패 - 이미 사용된 초대면 예외 (★ 1회용 검증)")
      void acceptInvite_이미사용됨_실패() {
          // given
          String token = "used.jwt.token";
          AcceptInviteRequest request = new AcceptInviteRequest(token);
          Long memberId = 2L;
          Long familyId = 100L;

          given(jwtUtil.getFamilyIdFromInviteToken(token)).willReturn(familyId);

          FamilyInvite usedInvite = FamilyInvite.builder()
                  .familyId(familyId)
                  .token(token)
                  .inviterId(1L)
                  .build();
          usedInvite.markAsUsed(99L);  // 다른 사람이 이미 사용

          given(familyInviteRepository.findByToken(token)).willReturn(Optional.of(usedInvite));

          // when & then
          assertThatThrownBy(() -> familyService.acceptInvite(request, memberId))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("이미 사용된 초대입니다");

          verify(familyMemberRepository, never()).save(any(FamilyMember.class));
      }

      @Test
      @DisplayName("초대 수락 실패 - 만료된 초대면 예외 (★ 만료 검증)")
      void acceptInvite_만료됨_실패() {
          // given
          String token = "expired.jwt.token";
          AcceptInviteRequest request = new AcceptInviteRequest(token);
          Long memberId = 2L;
          Long familyId = 100L;

          given(jwtUtil.getFamilyIdFromInviteToken(token)).willReturn(familyId);

          FamilyInvite expiredInvite = FamilyInvite.builder()
                  .familyId(familyId)
                  .token(token)
                  .inviterId(1L)
                  .build();
          ReflectionTestUtils.setField(expiredInvite, "expiresAt", LocalDateTime.now().minusHours(1));

          given(familyInviteRepository.findByToken(token)).willReturn(Optional.of(expiredInvite));

          // when & then
          assertThatThrownBy(() -> familyService.acceptInvite(request, memberId))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("만료된 초대입니다");

          verify(familyMemberRepository, never()).save(any(FamilyMember.class));
      }
  
      @Test
      void getDashboard_성공() {
          // given
          Long familyId = 1L;
          Long aliceId = 10L;
          Long bobId = 20L;

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, aliceId))
                  .willReturn(true);
                                                                                                                                  FamilyMember alicefm = FamilyMember.builder()
                  .familyId(familyId).memberId(aliceId).role("OWNER").build();
          FamilyMember bobfm = FamilyMember.builder()
                  .familyId(familyId).memberId(bobId).role("MEMBER").build();
          given(familyMemberRepository.findByFamilyId(familyId))
                  .willReturn(List.of(alicefm, bobfm));

          Member alice = Member.builder()
                  .email("alice@test.com").passwordHash("h").nickname("alice")
                  .birthDate(LocalDate.of(1995, 3, 15)).build();
          ReflectionTestUtils.setField(alice, "id", aliceId);

          Member bob = Member.builder()
                  .email("bob@test.com").passwordHash("h").nickname("bob")
                  .birthDate(LocalDate.of(1996, 7, 20)).build();
          ReflectionTestUtils.setField(bob, "id", bobId);

          given(memberRepository.findAllById(List.of(aliceId, bobId)))
                  .willReturn(List.of(alice, bob));

          // when
          List<DashboardMemberDto> result = familyService.getDashboard(familyId, aliceId);

          // then
          assertThat(result).hasSize(2);
          assertThat(result).extracting(DashboardMemberDto::nickname)
                  .containsExactlyInAnyOrder("alice", "bob");
          assertThat(result).extracting(DashboardMemberDto::presenceStatus)
                  .containsOnly("OUTSIDE");
      }

      @Test
      void getDashboard_가족멤버아님() {
          // given
          Long familyId = 1L;
          Long strangerId = 999L;

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, strangerId))
                  .willReturn(false);

          // when & then
          assertThatThrownBy(() -> familyService.getDashboard(familyId, strangerId))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("해당 가족의 멤버가 아닙니다");
      }
    	      
  }
  
  
  
  
  
  
  
  
  
  
  
  