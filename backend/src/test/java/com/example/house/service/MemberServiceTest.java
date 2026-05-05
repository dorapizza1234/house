package com.example.house.service;

  import com.example.house.domain.Member;
import com.example.house.dto.PresenceResponse;
import com.example.house.repository.MemberRepository;
  import org.junit.jupiter.api.Test;                                                                                      import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.test.util.ReflectionTestUtils;

  import java.time.LocalDateTime;
  import java.util.Optional;

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.mockito.BDDMockito.given;

  @ExtendWith(MockitoExtension.class)
  class MemberServiceTest {

      @Mock
      MemberRepository memberRepository;

      @InjectMocks
      MemberService memberService;

      @Test
      void togglePresence_성공() {
          // given
          Member member = Member.builder()
                  .email("alice@test.com")
                  .passwordHash("hashed")
                  .nickname("alice")
                  .birthDate(java.time.LocalDate.of(1995, 3, 15))
                  .build();
          ReflectionTestUtils.setField(member, "id", 1L);

          LocalDateTime before = member.getPresenceUpdatedAt();
          given(memberRepository.findById(1L)).willReturn(Optional.of(member));

          // when
          PresenceResponse response = memberService.togglePresence(1L);

          // then
          assertThat(member.getPresenceStatus()).isEqualTo("HOME");
          assertThat(member.getPresenceUpdatedAt()).isAfterOrEqualTo(before);
          assertThat(response.status()).isEqualTo("HOME");
          assertThat(response.updatedAt()).isEqualTo(member.getPresenceUpdatedAt());
      }

      @Test
      void togglePresence_회원없음() {
          // given
          given(memberRepository.findById(999L)).willReturn(Optional.empty());

          // when & then
          assertThatThrownBy(() -> memberService.togglePresence(999L))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("회원을 찾을 수 없습니다");
      }
  }
