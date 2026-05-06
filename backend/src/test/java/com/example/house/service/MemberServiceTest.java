package com.example.house.service;

  import com.example.house.domain.CheckInLog;
  import com.example.house.domain.FamilyMember;
  import com.example.house.domain.Member;
  import com.example.house.dto.PresenceResponse;                                                                          import com.example.house.repository.CheckInLogRepository;
  import com.example.house.repository.FamilyMemberRepository;                                                             import com.example.house.repository.MemberRepository;
  import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.ArgumentCaptor;
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
  import static org.mockito.BDDMockito.given;
  import static org.mockito.Mockito.never;
  import static org.mockito.Mockito.verify;

  @ExtendWith(MockitoExtension.class)
  class MemberServiceTest {

      @Mock MemberRepository memberRepository;
      @Mock FamilyMemberRepository familyMemberRepository;
      @Mock CheckInLogRepository checkInLogRepository;

      @InjectMocks MemberService memberService;

      @Test
      void togglePresence_성공() {
          // given
          Long memberId = 1L;
          Long familyId = 99L;

          Member member = Member.builder()
                  .email("alice@test.com")
                  .passwordHash("hashed")
                  .nickname("alice")
                  .birthDate(LocalDate.of(1995, 3, 15))
                  .build();
          ReflectionTestUtils.setField(member, "id", memberId);
          LocalDateTime before = member.getPresenceUpdatedAt();

          given(memberRepository.findById(memberId)).willReturn(Optional.of(member));

          FamilyMember fm = FamilyMember.builder()
                  .familyId(familyId).memberId(memberId).role("MEMBER").build();
          given(familyMemberRepository.findByMemberId(memberId)).willReturn(List.of(fm));

          // when
          PresenceResponse response = memberService.togglePresence(memberId);

          // then — Member 상태 검증
          assertThat(member.getPresenceStatus()).isEqualTo("HOME");
          assertThat(member.getPresenceUpdatedAt()).isAfterOrEqualTo(before);
          assertThat(response.status()).isEqualTo("HOME");

          // then — CheckInLog 저장 검증
          ArgumentCaptor<CheckInLog> captor = ArgumentCaptor.forClass(CheckInLog.class);
          verify(checkInLogRepository).save(captor.capture());
          CheckInLog saved = captor.getValue();
          assertThat(saved.getMemberId()).isEqualTo(memberId);
          assertThat(saved.getFamilyId()).isEqualTo(familyId);
          assertThat(saved.getStatus()).isEqualTo("HOME");
          assertThat(saved.getCheckedAt()).isEqualTo(member.getPresenceUpdatedAt());
      }

      @Test
      void togglePresence_가족없을때_familyIdNull() {
          // given
          Long memberId = 2L;

          Member member = Member.builder()
                  .email("bob@test.com")
                  .passwordHash("hashed")
                  .nickname("bob")
                  .birthDate(LocalDate.of(1996, 7, 20))
                  .build();
          ReflectionTestUtils.setField(member, "id", memberId);

          given(memberRepository.findById(memberId)).willReturn(Optional.of(member));
          given(familyMemberRepository.findByMemberId(memberId))
                  .willReturn(Collections.emptyList());

          // when
          memberService.togglePresence(memberId);

          // then — log의 familyId가 null
          ArgumentCaptor<CheckInLog> captor = ArgumentCaptor.forClass(CheckInLog.class);
          verify(checkInLogRepository).save(captor.capture());
          assertThat(captor.getValue().getFamilyId()).isNull();
      }

      @Test
      void togglePresence_회원없음() {
          // given
          given(memberRepository.findById(999L)).willReturn(Optional.empty());

          // when & then
          assertThatThrownBy(() -> memberService.togglePresence(999L))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("회원을 찾을 수 없습니다");

          // 회원 없을 때는 로그 저장도 안 일어나야 함
          verify(checkInLogRepository, never()).save(any());
      }
  }