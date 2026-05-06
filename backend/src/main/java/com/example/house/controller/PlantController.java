package com.example.house.controller;                                                                                 
  import com.example.house.domain.Member;                                                                                 import com.example.house.dto.PlantPlantRequest;
  import com.example.house.dto.PlantResponse;
  import com.example.house.repository.MemberRepository;
  import com.example.house.service.PlantService;
  import jakarta.validation.Valid;
  import lombok.RequiredArgsConstructor;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ResponseEntity;
  import org.springframework.security.core.annotation.AuthenticationPrincipal;
  import org.springframework.web.bind.annotation.GetMapping;
  import org.springframework.web.bind.annotation.PostMapping;
  import org.springframework.web.bind.annotation.RequestBody;
  import org.springframework.web.bind.annotation.RequestMapping;
  import org.springframework.web.bind.annotation.RestController;

  @RestController
  @RequestMapping("/api/families/me/plant")
  @RequiredArgsConstructor
  public class PlantController {

      private final PlantService plantService;
      private final MemberRepository memberRepository;

      // 식물 심기
      @PostMapping
      public ResponseEntity<PlantResponse> plantPlant(
              @Valid @RequestBody PlantPlantRequest request,
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          PlantResponse response = plantService.plantPlant(memberId, request);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
      }

      // 가족 식물 조회
      @GetMapping
      public ResponseEntity<PlantResponse> getMyFamilyPlant(
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          PlantResponse response = plantService.getMyFamilyPlant(memberId);
          return ResponseEntity.ok(response);
      }

      // 물주기
      @PostMapping("/water")
      public ResponseEntity<PlantResponse> waterPlant(
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          PlantResponse response = plantService.waterPlant(memberId);
          return ResponseEntity.ok(response);
      }

      // 부활
      @PostMapping("/revive")
      public ResponseEntity<PlantResponse> revivePlant(
              @AuthenticationPrincipal String email
      ) {
          Long memberId = resolveMemberId(email);
          PlantResponse response = plantService.revivePlant(memberId);
          return ResponseEntity.ok(response);
      }

      private Long resolveMemberId(String email) {
          Member member = memberRepository.findByEmail(email)
                  .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다"));
          return member.getId();
      }
  }
