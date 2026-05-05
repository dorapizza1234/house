  package com.example.house.controller;

  import com.example.house.domain.Member;
  import com.example.house.dto.AcceptInviteRequest;
  import com.example.house.dto.AcceptInviteResponse;
  import com.example.house.dto.CreateFamilyRequest;
  import com.example.house.dto.CreateFamilyResponse;
  import com.example.house.dto.CreateInviteResponse;
  import com.example.house.repository.MemberRepository;
  import com.example.house.service.FamilyService;
  import jakarta.validation.Valid;
  import lombok.RequiredArgsConstructor;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ResponseEntity;
  import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
  import org.springframework.web.bind.annotation.PostMapping;
  import org.springframework.web.bind.annotation.RequestBody;
  import org.springframework.web.bind.annotation.RequestMapping;
  import org.springframework.web.bind.annotation.RestController;
  import com.example.house.dto.DashboardMemberDto;
  import org.springframework.web.bind.annotation.GetMapping;
  import java.util.List;
  
  @RestController
  @RequestMapping("/api/families")
  @RequiredArgsConstructor
  public class FamilyController {

      private final FamilyService familyService;
      private final MemberRepository memberRepository;

      // 가족 생성
      @PostMapping
      public ResponseEntity<CreateFamilyResponse> createFamily(
              @Valid @RequestBody CreateFamilyRequest request,
              @AuthenticationPrincipal String email
      ) {
          Long creatorId = resolveMemberId(email);
          CreateFamilyResponse response = familyService.createFamily(request, creatorId);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
      }

      // 초대 생성
      @PostMapping("/{familyId}/invites")
      public ResponseEntity<CreateInviteResponse> createInvite(
    		  @PathVariable("familyId") Long familyId,
              @AuthenticationPrincipal String email
      ) {
          Long inviterId = resolveMemberId(email);
          CreateInviteResponse response = familyService.createInvite(familyId, inviterId);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
      }

      // 초대 수락
      @PostMapping("/invites/accept")
      public ResponseEntity<AcceptInviteResponse> acceptInvite(
              @Valid @RequestBody AcceptInviteRequest request,
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          AcceptInviteResponse response = familyService.acceptInvite(request, memberId);
          return ResponseEntity.ok(response);
      }

      // 인증된 이메일로 회원 ID 조회 (JWT subject가 email이라 변환 필요)
      private Long resolveMemberId(String email) {
          Member member = memberRepository.findByEmail(email)
                  .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다"));
          return member.getId();
      }
      
      @GetMapping("/{familyId}/dashboard")
      public ResponseEntity<List<DashboardMemberDto>> getDashboard(
              @PathVariable("familyId") Long familyId,
              @AuthenticationPrincipal String email) {

          Long memberId = resolveMemberId(email);                                                                                 List<DashboardMemberDto> dashboard = familyService.getDashboard(familyId, memberId);
          return ResponseEntity.ok(dashboard);
      }
      
      
      
      
      
      
  }
