package com.example.house.service;

  import com.example.house.domain.FamilyEvent;
  import com.example.house.dto.CalendarResponse;
  import com.example.house.dto.CreateEventRequest;
  import com.example.house.dto.EventResponse;
  import com.example.house.repository.FamilyEventRepository;
  import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.MemberRepository;                                                                   import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;                                                                          import org.springframework.transaction.annotation.Transactional;
  import com.example.house.domain.FamilyMember;
  import com.example.house.domain.Member;
  import com.example.house.dto.CalendarItemDto;
  import com.example.house.dto.CalendarResponse;

  import java.time.DateTimeException;
  import java.time.LocalDate;
  import java.util.ArrayList;
  import java.util.Comparator;
  import java.util.List;
  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  public class CalendarService {

      private final FamilyEventRepository familyEventRepository;
      private final FamilyMemberRepository familyMemberRepository;
      private final MemberRepository memberRepository;
     
     
      @Transactional
      public EventResponse createEvent(Long familyId, Long memberId, CreateEventRequest request) {
          if (!familyMemberRepository.existsByFamilyIdAndMemberId(familyId, memberId)) {
              throw new IllegalArgumentException("해당 가족의 멤버가 아닙니다");
          }

          FamilyEvent event = FamilyEvent.builder()
                  .familyId(familyId)
                  .type(request.type())
                  .title(request.title())
                  .eventDate(request.eventDate())
                  .isYearly(request.isYearly())
                  .creatorId(memberId)
                  .memo(request.memo())
                  .build();

          FamilyEvent saved = familyEventRepository.save(event);

          return new EventResponse(
                  saved.getId(),
                  saved.getType(),
                  saved.getTitle(),
                  saved.getEventDate(),
                  saved.isYearly(),
                  saved.getMemo(),
                  saved.getCreatorId(),
                  saved.getCreatedAt()
          );
      }
      
      
      public CalendarResponse getCalendar(Long familyId, Long memberId, int year, int month) {
          if (!familyMemberRepository.existsByFamilyIdAndMemberId(familyId, memberId)) {
              throw new IllegalArgumentException("해당 가족의 멤버가 아닙니다");
          }
      
          List<CalendarItemDto> items = new ArrayList<>();

          // 1. 가족 이벤트 조회 + 월 필터링         
          List<FamilyEvent> events = familyEventRepository.findByFamilyId(familyId);
          for (FamilyEvent event : events) {                                                                                          LocalDate occurrence = computeOccurrence(event, year, month);
              if (occurrence != null) {
                  items.add(new CalendarItemDto(
                          event.getId(),
                          event.getType(),
                          event.getTitle(),
                          occurrence,
                          event.isYearly(),
                          event.getMemo(),
                          event.getCreatorId()
                  ));
              }
          }

          // 2. 생일 계산 (가족 멤버들 birth_date)
          List<FamilyMember> familyMembers = familyMemberRepository.findByFamilyId(familyId);
          List<Long> memberIds = familyMembers.stream()
                  .map(FamilyMember::getMemberId)
                  .toList();
          List<Member> members = memberRepository.findAllById(memberIds);

          for (Member m : members) {
              if (m.getBirthDate() == null) continue;
              if (m.getBirthDate().getMonthValue() != month) continue;

              try {
                  LocalDate birthdayThisYear = LocalDate.of(year, month, m.getBirthDate().getDayOfMonth());
                  items.add(new CalendarItemDto(
                          null,
                          "BIRTHDAY",
                          m.getNickname() + " 생일",
                          birthdayThisYear,
                          true,
                          null,
                          null
                  ));
              } catch (DateTimeException ignored) {
                  // 2/29 생인이 평년에 떨어질 때 — 일단 스킵
              }
          }

          // 3. 날짜순 정렬
          items.sort(Comparator.comparing(CalendarItemDto::date));

          return new CalendarResponse(year, month, items);
      }

      private LocalDate computeOccurrence(FamilyEvent event, int year, int month) {
          LocalDate eventDate = event.getEventDate();
          if (event.isYearly()) {
              if (eventDate.getMonthValue() == month) {
                  try {
                      return LocalDate.of(year, month, eventDate.getDayOfMonth());
                  } catch (DateTimeException e) {
                      return null;  // 2/29 매년반복 — 평년 스킵
                  }
              }
              return null;
          } else {
              if (eventDate.getYear() == year && eventDate.getMonthValue() == month) {
                  return eventDate;
              }
              return null;
          }
      }
      
      @Transactional
      public void deleteEvent(Long eventId, Long memberId) {
          FamilyEvent event = familyEventRepository.findById(eventId)
                  .orElseThrow(() -> new IllegalArgumentException("이벤트를 찾을 수 없습니다"));                            
          if (!event.isOwnedBy(memberId)) {                                                                                           throw new IllegalArgumentException("작성자만 삭제할 수 있습니다");
          }

          familyEventRepository.delete(event);
      }
      
      
      
      
      
  }

