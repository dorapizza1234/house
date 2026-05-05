package com.example.house.service;

  import com.example.house.domain.FamilyEvent;
  import com.example.house.dto.CreateEventRequest;
  import com.example.house.dto.EventResponse;
  import com.example.house.repository.FamilyEventRepository;
  import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.MemberRepository;
import com.example.house.service.CalendarService;

import org.junit.jupiter.api.Test;
  import org.junit.jupiter.api.extension.ExtendWith;
  import org.mockito.InjectMocks;
  import org.mockito.Mock;
  import org.mockito.junit.jupiter.MockitoExtension;
  import org.springframework.test.util.ReflectionTestUtils;

  import java.time.LocalDate;
  import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.BDDMockito.given;
  import com.example.house.domain.FamilyMember;
  import com.example.house.domain.Member;
  import com.example.house.dto.CalendarItemDto;
  import com.example.house.dto.CalendarResponse;
  import com.example.house.domain.FamilyEvent;
  import java.util.Optional;
  import static org.mockito.Mockito.never;
  import static org.mockito.Mockito.verify;
  
  @ExtendWith(MockitoExtension.class)
  class CalendarServiceTest {

      @Mock FamilyEventRepository familyEventRepository;
      @Mock FamilyMemberRepository familyMemberRepository;
      @Mock MemberRepository memberRepository;

      @InjectMocks CalendarService calendarService;

      @Test
      void createEvent_성공() {
          // given
          Long familyId = 1L;
          Long memberId = 10L;
          CreateEventRequest req = new CreateEventRequest(
                  "JESA", "할머니 제사", LocalDate.of(2026, 5, 10), true, "추모 메모"
          );

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, memberId))
                  .willReturn(true);

          FamilyEvent saved = FamilyEvent.builder()
                  .familyId(familyId)
                  .type(req.type())
                  .title(req.title())
                  .eventDate(req.eventDate())
                  .isYearly(req.isYearly())
                  .creatorId(memberId)
                  .memo(req.memo())
                  .build();
          ReflectionTestUtils.setField(saved, "id", 100L);
          ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.now());

          given(familyEventRepository.save(any(FamilyEvent.class))).willReturn(saved);

          // when
          EventResponse response = calendarService.createEvent(familyId, memberId, req);

          // then
          assertThat(response.id()).isEqualTo(100L);
          assertThat(response.type()).isEqualTo("JESA");
          assertThat(response.title()).isEqualTo("할머니 제사");
          assertThat(response.isYearly()).isTrue();
          assertThat(response.creatorId()).isEqualTo(memberId);
      }

      @Test
      void createEvent_가족멤버아님() {
          // given
          Long familyId = 1L;
          Long strangerId = 999L;
          CreateEventRequest req = new CreateEventRequest(
                  "SCHEDULE", "회식", LocalDate.of(2026, 5, 10), false, null
          );

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, strangerId))
                  .willReturn(false);

          // when & then
          assertThatThrownBy(() -> calendarService.createEvent(familyId, strangerId, req))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("해당 가족의 멤버가 아닙니다");
      }
      @Test
      void getCalendar_성공_이벤트와생일포함() {
          // given
          Long familyId = 1L;
          Long aliceId = 10L;
          int year = 2026;
          int month = 5;

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, aliceId))
                  .willReturn(true);

          // 매년반복 이벤트 (5월에 떨어짐)
          FamilyEvent jesa = FamilyEvent.builder()
                  .familyId(familyId).type("JESA").title("할머니 제사")
                  .eventDate(LocalDate.of(2024, 5, 10)).isYearly(true)
                  .creatorId(aliceId).memo(null).build();
          ReflectionTestUtils.setField(jesa, "id", 100L);

          // 일회성 이벤트 (정확히 2026-05)
          FamilyEvent meeting = FamilyEvent.builder()
                  .familyId(familyId).type("SCHEDULE").title("가족 모임")
                  .eventDate(LocalDate.of(2026, 5, 20)).isYearly(false)
                  .creatorId(aliceId).memo(null).build();
          ReflectionTestUtils.setField(meeting, "id", 101L);

          // 다른 달 이벤트 (제외되어야 함)
          FamilyEvent other = FamilyEvent.builder()
                  .familyId(familyId).type("SCHEDULE").title("3월 약속")
                  .eventDate(LocalDate.of(2026, 3, 1)).isYearly(false)
                  .creatorId(aliceId).memo(null).build();
          ReflectionTestUtils.setField(other, "id", 102L);

          given(familyEventRepository.findByFamilyId(familyId))
                  .willReturn(List.of(jesa, meeting, other));

          // 가족 멤버 1명 (5월 생일)
          FamilyMember aliceFm = FamilyMember.builder()
                  .familyId(familyId).memberId(aliceId).role("OWNER").build();
          given(familyMemberRepository.findByFamilyId(familyId))
                  .willReturn(List.of(aliceFm));

          Member alice = Member.builder()
                  .email("alice@test.com").passwordHash("h").nickname("alice")
                  .birthDate(LocalDate.of(1995, 5, 15)).build();
          ReflectionTestUtils.setField(alice, "id", aliceId);

          given(memberRepository.findAllById(List.of(aliceId)))
                  .willReturn(List.of(alice));

          // when
          CalendarResponse response = calendarService.getCalendar(familyId, aliceId, year, month);

          // then
          assertThat(response.year()).isEqualTo(year);
          assertThat(response.month()).isEqualTo(month);
          assertThat(response.items()).hasSize(3);  // 제사 + 모임 + 생일
          assertThat(response.items()).extracting(CalendarItemDto::type)
                  .containsExactly("JESA", "BIRTHDAY", "SCHEDULE");  // 날짜순: 5/10, 5/15, 5/20
          assertThat(response.items().get(0).date()).isEqualTo(LocalDate.of(2026, 5, 10));
          assertThat(response.items().get(1).title()).isEqualTo("alice 생일");
          assertThat(response.items().get(2).date()).isEqualTo(LocalDate.of(2026, 5, 20));
      }

      @Test
      void getCalendar_가족멤버아님() {
          // given
          Long familyId = 1L;
          Long strangerId = 999L;

          given(familyMemberRepository.existsByFamilyIdAndMemberId(familyId, strangerId))
                  .willReturn(false);

          // when & then
          assertThatThrownBy(() -> calendarService.getCalendar(familyId, strangerId, 2026, 5))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("해당 가족의 멤버가 아닙니다");
      }
      @Test
      void deleteEvent_성공() {
          // given
          Long eventId = 100L;
          Long aliceId = 10L;

          FamilyEvent event = FamilyEvent.builder()
                  .familyId(1L).type("JESA").title("할머니 제사")
                  .eventDate(LocalDate.of(2024, 5, 10)).isYearly(true)
                  .creatorId(aliceId).memo(null).build();
          ReflectionTestUtils.setField(event, "id", eventId);

          given(familyEventRepository.findById(eventId)).willReturn(Optional.of(event));

          // when
          calendarService.deleteEvent(eventId, aliceId);

          // then
          verify(familyEventRepository).delete(event);
      }

      @Test
      void deleteEvent_이벤트없음() {
          // given
          Long eventId = 999L;
          given(familyEventRepository.findById(eventId)).willReturn(Optional.empty());

          // when & then
          assertThatThrownBy(() -> calendarService.deleteEvent(eventId, 10L))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("이벤트를 찾을 수 없습니다");
      }

      @Test
      void deleteEvent_작성자아님() {
          // given
          Long eventId = 100L;
          Long aliceId = 10L;
          Long bobId = 20L;  // 다른 사람

          FamilyEvent event = FamilyEvent.builder()
                  .familyId(1L).type("JESA").title("할머니 제사")
                  .eventDate(LocalDate.of(2024, 5, 10)).isYearly(true)
                  .creatorId(aliceId).memo(null).build();
          ReflectionTestUtils.setField(event, "id", eventId);

          given(familyEventRepository.findById(eventId)).willReturn(Optional.of(event));

          // when & then
          assertThatThrownBy(() -> calendarService.deleteEvent(eventId, bobId))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("작성자만 삭제할 수 있습니다");

          verify(familyEventRepository, never()).delete(event);
      }

      
      
  }