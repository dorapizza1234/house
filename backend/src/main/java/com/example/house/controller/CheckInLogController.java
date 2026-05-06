  package com.example.house.controller;
                                                                                                                          import com.example.house.domain.Member;
  import com.example.house.dto.CheckInLogResponse;
  import com.example.house.repository.MemberRepository;
  import com.example.house.service.CheckInLogService;
  import lombok.RequiredArgsConstructor;
  import org.springframework.http.ResponseEntity;
  import org.springframework.security.core.annotation.AuthenticationPrincipal;
  import org.springframework.web.bind.annotation.GetMapping;
  import org.springframework.web.bind.annotation.PathVariable;
  import org.springframework.web.bind.annotation.RequestMapping;
  import org.springframework.web.bind.annotation.RequestParam;
  import org.springframework.web.bind.annotation.RestController;

  import java.util.List;

  @RestController
  @RequiredArgsConstructor
  public class CheckInLogController {

      private final CheckInLogService checkInLogService;
      private final MemberRepository memberRepository;

      // 본인 로그 조회
      @GetMapping("/api/me/check-in-logs")
      public ResponseEntity<List<CheckInLogResponse>> getMyLogs(
    		  @RequestParam(name = "days", defaultValue = "7") int days,
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          List<CheckInLogResponse> logs = checkInLogService.getMyLogs(memberId, days);
          return ResponseEntity.ok(logs);
      }

      // 가족 로그 조회
      @GetMapping("/api/families/{familyId}/check-in-logs")
      public ResponseEntity<List<CheckInLogResponse>> getFamilyLogs(
              @PathVariable("familyId") Long familyId,
              @RequestParam(name = "days", defaultValue = "7") int days,
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          List<CheckInLogResponse> logs = checkInLogService.getFamilyLogs(familyId, memberId, days);
          return ResponseEntity.ok(logs);
      }

      private Long resolveMemberId(String email) {
          Member member = memberRepository.findByEmail(email)
                  .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다"));
          return member.getId();
      }
  }