package com.example.house.controller;

import com.example.house.domain.Member;
import com.example.house.dto.CalendarResponse;                                                                          import com.example.house.dto.CreateEventRequest;
import com.example.house.dto.EventResponse;                                                                             import com.example.house.repository.MemberRepository;
import com.example.house.service.CalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/families/{familyId}")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;
    private final MemberRepository memberRepository;

    @PostMapping("/events")
    public ResponseEntity<EventResponse> createEvent(
            @PathVariable("familyId") Long familyId,
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal String email) {

        Long memberId = resolveMemberId(email);
        EventResponse response = calendarService.createEvent(familyId, memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/calendar")
    public ResponseEntity<CalendarResponse> getCalendar(
            @PathVariable("familyId") Long familyId,
            @RequestParam("year") int year,
            @RequestParam("month") int month,
            @AuthenticationPrincipal String email) {

        Long memberId = resolveMemberId(email);
        CalendarResponse response = calendarService.getCalendar(familyId, memberId, year, month);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/events/{eventId}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable("familyId") Long familyId,
            @PathVariable("eventId") Long eventId,
            @AuthenticationPrincipal String email) {

        Long memberId = resolveMemberId(email);
        calendarService.deleteEvent(eventId, memberId);
        return ResponseEntity.noContent().build();
    }

    private Long resolveMemberId(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));
        return member.getId();
    }
}
