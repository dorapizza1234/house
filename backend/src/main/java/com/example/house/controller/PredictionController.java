  package com.example.house.controller;

  import com.example.house.domain.Member;
  import com.example.house.dto.HourPredictionResponse;
  import com.example.house.repository.MemberRepository;
  import com.example.house.service.PredictionService;
  import lombok.RequiredArgsConstructor;
  import org.springframework.http.ResponseEntity;
  import org.springframework.security.core.annotation.AuthenticationPrincipal;
  import org.springframework.web.bind.annotation.GetMapping;
  import org.springframework.web.bind.annotation.RequestMapping;
  import org.springframework.web.bind.annotation.RequestParam;
  import org.springframework.web.bind.annotation.RestController;
  import com.example.house.dto.TodayComparisonResponse;
  @RestController
  @RequestMapping("/api/me/predictions")
  @RequiredArgsConstructor
  public class PredictionController {

      private final PredictionService predictionService;
      private final MemberRepository memberRepository;

      @GetMapping("/leave-time")
      public ResponseEntity<HourPredictionResponse> getLeaveTime(
              @RequestParam(name = "days", defaultValue = "7") int days,
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          HourPredictionResponse response = predictionService.getAverageLeaveTime(memberId, days);
          return ResponseEntity.ok(response);
      }

      @GetMapping("/return-time")
      public ResponseEntity<HourPredictionResponse> getReturnTime(
              @RequestParam(name = "days", defaultValue = "7") int days,
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          HourPredictionResponse response = predictionService.getAverageReturnTime(memberId, days);
          return ResponseEntity.ok(response);
      }

      private Long resolveMemberId(String email) {
          Member member = memberRepository.findByEmail(email)
                  .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다"));
          return member.getId();
      }
      
      @GetMapping("/today-comparison")
      public ResponseEntity<TodayComparisonResponse> getTodayComparison(
              @RequestParam(name = "days", defaultValue = "7") int days,
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          TodayComparisonResponse response = predictionService.getTodayReturnComparison(memberId, days);
          return ResponseEntity.ok(response);
      }
  
    } 
    
    
    
	
	
	

