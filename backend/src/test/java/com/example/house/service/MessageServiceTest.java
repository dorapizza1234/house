package com.example.house.service;

  import com.example.house.domain.FamilyEvent;
  import com.example.house.domain.FamilyMember;
  import com.example.house.domain.Message;
  import com.example.house.domain.SpecialHoliday;
  import com.example.house.dto.SendMessageRequest;
  import com.example.house.dto.SendMessageResponse;
  import com.example.house.repository.FamilyEventRepository;
  import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.MessageRepository;
  import com.example.house.repository.MessageTypeRepository;
  import com.example.house.repository.SpecialHolidayRepository;
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

  import static org.assertj.core.api.Assertions.assertThat;
  import static org.assertj.core.api.Assertions.assertThatThrownBy;
  import static org.mockito.ArgumentMatchers.any;
  import static org.mockito.ArgumentMatchers.anyInt;
  import static org.mockito.ArgumentMatchers.eq;
  import static org.mockito.BDDMockito.given;

  @ExtendWith(MockitoExtension.class)
  class MessageServiceTest {

      @Mock MessageRepository messageRepository;
      @Mock MessageTypeRepository messageTypeRepository;
      @Mock SpecialHolidayRepository specialHolidayRepository;
      @Mock FamilyMemberRepository familyMemberRepository;
      @Mock FamilyEventRepository familyEventRepository;

      @InjectMocks MessageService messageService;

      private static final Long SENDER_ID = 1L;
      private static final Long RECEIVER_ID = 2L;
      private static final Long FAMILY_ID = 10L;
      private static final Long PRESET_TYPE_ID = 1L;

      @Test
      void sendMessage_평일프리셋_10점() {
          FamilyMember sender = setupSender();
          setupReceiver();
          setupPresetTypeValid();
          setupTodayCount(0L);
          setupNoEventsToday();
          setupSaveMessage();

          SendMessageRequest req = new SendMessageRequest(RECEIVER_ID, PRESET_TYPE_ID, "감사합니다");

          SendMessageResponse result = messageService.sendMessage(SENDER_ID, req);

          assertThat(result.finalPoints()).isEqualTo(10);
          assertThat(result.multiplier()).isEqualTo(1);
          assertThat(result.eventReasons()).isNull();
          assertThat(result.todayPresetCount()).isEqualTo(1);
          assertThat(sender.getPoints()).isEqualTo(10);  // dirty checking
      }

      @Test
      void sendMessage_자유입력_0점() {
          FamilyMember sender = setupSender();
          setupReceiver();
          setupTodayCount(0L);
          setupNoEventsToday();
          setupSaveMessage();

          SendMessageRequest req = new SendMessageRequest(RECEIVER_ID, null, "오늘 어땠어요");

          SendMessageResponse result = messageService.sendMessage(SENDER_ID, req);

          assertThat(result.finalPoints()).isZero();
          assertThat(result.multiplier()).isEqualTo(1);
          assertThat(result.todayPresetCount()).isZero();  // 프리셋 아니라 카운트 안 늘어남
          assertThat(sender.getPoints()).isZero();
      }

      @Test
      void sendMessage_공휴일1개_20점() {
          FamilyMember sender = setupSender();
          setupReceiver();
          setupPresetTypeValid();
          setupTodayCount(0L);
          SpecialHoliday parentsDay = SpecialHoliday.builder()
                  .code("PARENTS_DAY").nameKo("어버이날").month(5).day(8).build();
          given(specialHolidayRepository.findByMonthAndDay(anyInt(), anyInt()))
                  .willReturn(List.of(parentsDay));
          given(familyEventRepository.findByFamilyId(FAMILY_ID))
                  .willReturn(Collections.emptyList());
          setupSaveMessage();

          SendMessageRequest req = new SendMessageRequest(RECEIVER_ID, PRESET_TYPE_ID, "감사합니다");

          SendMessageResponse result = messageService.sendMessage(SENDER_ID, req);

          assertThat(result.finalPoints()).isEqualTo(20);
          assertThat(result.multiplier()).isEqualTo(2);
          assertThat(result.eventReasons()).isEqualTo("어버이날");
          assertThat(sender.getPoints()).isEqualTo(20);
      }

      @Test
      void sendMessage_공휴일과가족이벤트2개_40점() {
          FamilyMember sender = setupSender();
          setupReceiver();
          setupPresetTypeValid();
          setupTodayCount(0L);
          SpecialHoliday parentsDay = SpecialHoliday.builder()
                  .code("PARENTS_DAY").nameKo("어버이날").month(5).day(8).build();
          given(specialHolidayRepository.findByMonthAndDay(anyInt(), anyInt()))
                  .willReturn(List.of(parentsDay));
          FamilyEvent momBirthday = FamilyEvent.builder()
                  .familyId(FAMILY_ID).type("BIRTHDAY").title("엄마생신")
                  .eventDate(LocalDate.now()).isYearly(true).creatorId(SENDER_ID).memo(null).build();
          given(familyEventRepository.findByFamilyId(FAMILY_ID))
                  .willReturn(List.of(momBirthday));
          setupSaveMessage();

          SendMessageRequest req = new SendMessageRequest(RECEIVER_ID, PRESET_TYPE_ID, "감사합니다");

          SendMessageResponse result = messageService.sendMessage(SENDER_ID, req);

          assertThat(result.finalPoints()).isEqualTo(40);
          assertThat(result.multiplier()).isEqualTo(4);
          assertThat(result.eventReasons()).contains("어버이날", "엄마생신");
          assertThat(sender.getPoints()).isEqualTo(40);
      }

      @Test
      void sendMessage_하루한도초과_예외() {
          setupSender();
          setupReceiver();
          setupPresetTypeValid();
          setupTodayCount(3L);  // 이미 3개 보냄

          SendMessageRequest req = new SendMessageRequest(RECEIVER_ID, PRESET_TYPE_ID, "감사합니다");

          assertThatThrownBy(() -> messageService.sendMessage(SENDER_ID, req))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessageContaining("한도");
      }

      @Test
      void sendMessage_가족멤버아닌사람_예외() {
          setupSender();
          given(familyMemberRepository.existsByFamilyIdAndMemberId(FAMILY_ID, RECEIVER_ID))
                  .willReturn(false);

          SendMessageRequest req = new SendMessageRequest(RECEIVER_ID, PRESET_TYPE_ID, "감사");

          assertThatThrownBy(() -> messageService.sendMessage(SENDER_ID, req))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("같은 가족 멤버가 아닙니다");
      }

      @Test
      void sendMessage_가족없음_예외() {
          given(familyMemberRepository.findByMemberId(SENDER_ID))
                  .willReturn(Collections.emptyList());

          SendMessageRequest req = new SendMessageRequest(RECEIVER_ID, PRESET_TYPE_ID, "감사");

          assertThatThrownBy(() -> messageService.sendMessage(SENDER_ID, req))
                  .isInstanceOf(IllegalArgumentException.class)
                  .hasMessage("가족이 없습니다");
      }

      // ========= 헬퍼들 =========

      private FamilyMember setupSender() {
          FamilyMember sender = FamilyMember.builder()
                  .familyId(FAMILY_ID).memberId(SENDER_ID).role("MEMBER").build();
          ReflectionTestUtils.setField(sender, "id", 100L);
          given(familyMemberRepository.findByMemberId(SENDER_ID)).willReturn(List.of(sender));
          return sender;
      }

      private void setupReceiver() {
          given(familyMemberRepository.existsByFamilyIdAndMemberId(FAMILY_ID, RECEIVER_ID))
                  .willReturn(true);
      }

      private void setupPresetTypeValid() {
          given(messageTypeRepository.existsById(PRESET_TYPE_ID)).willReturn(true);
      }

      private void setupTodayCount(long count) {
          given(messageRepository.countBySenderIdAndMessageTypeIdIsNotNullAndCreatedAtBetween(
                  eq(SENDER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                  .willReturn(count);
      }

      private void setupNoEventsToday() {
          given(specialHolidayRepository.findByMonthAndDay(anyInt(), anyInt()))
                  .willReturn(Collections.emptyList());
          given(familyEventRepository.findByFamilyId(FAMILY_ID))
                  .willReturn(Collections.emptyList());
      }

      private void setupSaveMessage() {
          given(messageRepository.save(any(Message.class))).willAnswer(invocation -> {
              Message m = invocation.getArgument(0);
              ReflectionTestUtils.setField(m, "id", 999L);
              return m;
          });
      }
  }
