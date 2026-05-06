package com.example.house.service;

  import com.example.house.domain.CheckInLog;
  import com.example.house.domain.Member;
  import com.example.house.dto.CheckInLogResponse;
  import com.example.house.repository.CheckInLogRepository;
  import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.MemberRepository;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.test.util.ReflectionTestUtils;

  import java.time.LocalDate;
  import java.time.LocalDateTime;
  import java.util.List;
  import java.util.Optional;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.ArgumentMatchers.eq;
  import static org.mockito.BDDMockito.given;

  @ExtendWith(MockitoExtension.class)
  class CheckInLogServiceTest {

      @Mock CheckInLogRepository checkInLogRepository;
      @Mock MemberRepository memberRepository;
      @Mock FamilyMemberRepository familyMemberRepository;

      @InjectMocks CheckInLogService checkInLogService;

      @Test
      void getMyLogs_성공() {
          // given
          Long memberId = 1L;
          int days = 7;

          Member member = Member.builder()
                  .email("alice@test.com").passwordHash("h").nickname("alice")
                  .birthDate(LocalDate.of(1995, 3, 15)).build();
          ReflectionTestUtils.setField(member, "id", memberId);

          given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

          CheckInLog log1 = CheckInLog.builder()
                  .memberId(memberId).familyId(99L).status("HOME")
                  .checkedAt(LocalDateTime.now().minusHours(2)).build();
          ReflectionTestUtils.setField(log1, "id", 100L);

          CheckInLog log2 = CheckInLog.builder()
                  .memberId(memberId).familyId(99L).status("OUTSIDE")
                  .checkedAt(LocalDateTime.now().minusHours(10)).build();
          ReflectionTestUtils.setField(log2, "id", 101L);

          given(checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(memberId), any(LocalDateTime.class)))
                  .willReturn(List.of(log1, log2));

          // when
          List<CheckInLogResponse> result = checkInLogService.getMyLogs(memberId, days);

          // then
          assertThat(result).hasSize(2);
          assertThat(result).extracting(CheckInLogResponse::memberNickname)
                  .containsOnly("alice");
          assertThat(result).extracting(CheckInLogResponse::status)
                  .containsExactly("HOME", "OUTSIDE");  // DESC 순
          assertThat(result.get(0).id()).isEqualTo(100L);
      }

      @Test
      void getMyLogs_days범위벗어남() {
          // when & then
          assertThatThrownBy(() -> checkInLogService.getMyLogs(1L, 0))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("days는 1~30 사이여야 합니다");

          assertThatThrownBy(() -> checkInLogService.getMyLogs(1L, 31))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("days는 1~30 사이여야 합니다");
      }
      
      
      @Test
      void getFamilyLogs_성공() {
          // given
          Long familyId = 10L;
          Long viewerId = 1L;  // 조회하는 사람
          int days = 7;

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, viewerId))
                  .willReturn(true);

          CheckInLog aliceLog = CheckInLog.builder()
                  .memberId(1L).familyId(familyId).status("HOME")
                  .checkedAt(LocalDateTime.now().minusHours(1)).build();
          ReflectionTestUtils.setField(aliceLog, "id", 100L);

          CheckInLog bobLog = CheckInLog.builder()
                  .memberId(2L).familyId(familyId).status("OUTSIDE")
                  .checkedAt(LocalDateTime.now().minusHours(3)).build();
          ReflectionTestUtils.setField(bobLog, "id", 101L);

          given(checkInLogRepository
                  .findByFamilyIdAndCheckedAtAfterOrderByCheckedAtDesc(eq(familyId), any(LocalDateTime.class)))
                  .willReturn(List.of(aliceLog, bobLog));

          Member alice = Member.builder()
                  .email("a@t.com").passwordHash("h").nickname("alice")
                  .birthDate(LocalDate.of(1995, 3, 15)).build();
          ReflectionTestUtils.setField(alice, "id", 1L);

          Member bob = Member.builder()
                  .email("b@t.com").passwordHash("h").nickname("bob")
                  .birthDate(LocalDate.of(1990, 5, 20)).build();
          ReflectionTestUtils.setField(bob, "id", 2L);

          given(memberRepository.findAllById(List.of(1L, 2L)))
                  .willReturn(List.of(alice, bob));

          // when
          List<CheckInLogResponse> result = checkInLogService.getFamilyLogs(familyId, viewerId, days);

          // then
          assertThat(result).hasSize(2);
          assertThat(result).extracting(CheckInLogResponse::memberNickname)
                  .containsExactly("alice", "bob");  // DESC 순
          assertThat(result).extracting(CheckInLogResponse::status)
                  .containsExactly("HOME", "OUTSIDE");
      }

      @Test
      void getFamilyLogs_가족멤버아님() {
          // given
          given(familyMemberRepository.existsByFamilyIdAndMemberId(10L, 99L))
                  .willReturn(false);

          // when & then
          assertThatThrownBy(() -> checkInLogService.getFamilyLogs(10L, 99L, 7))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("가족 멤버가 아닙니다");
      }
  }