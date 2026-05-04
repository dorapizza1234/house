package com.example.house.service;

  import com.example.house.domain.Family;
import com.example.house.domain.FamilyInvite;
import com.example.house.domain.FamilyMember;
import com.example.house.dto.AcceptInviteRequest;
import com.example.house.dto.AcceptInviteResponse;
import com.example.house.dto.CreateFamilyRequest;
  import com.example.house.dto.CreateFamilyResponse;
import com.example.house.dto.CreateInviteResponse;
import com.example.house.repository.FamilyInviteRepository;
  import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.FamilyRepository;
  import com.example.house.security.JwtUtil;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  @Service
  @RequiredArgsConstructor
  public class FamilyService {

      private final FamilyRepository familyRepository;
      private final FamilyMemberRepository familyMemberRepository;
      private final FamilyInviteRepository familyInviteRepository;
      private final JwtUtil jwtUtil;

      // 가족 생성 + 생성자를 OWNER로 자동 등록
      @Transactional
      public CreateFamilyResponse createFamily(CreateFamilyRequest request, Long creatorId) {
          Family family = Family.builder()
                  .name(request.name())
                  .creatorId(creatorId)
                  .build();
          Family saved = familyRepository.save(family);

          FamilyMember owner = FamilyMember.builder()
                  .familyId(saved.getId())
                  .memberId(creatorId)
                  .role("OWNER")
                  .build();
          familyMemberRepository.save(owner);

          return new CreateFamilyResponse(saved.getId(), saved.getName());
      }
      
      @Transactional
      public CreateInviteResponse createInvite(Long familyId, Long inviterId) {
          if (!familyMemberRepository.existsByFamilyIdAndMemberId(familyId, inviterId)) {
              throw new IllegalArgumentException("가족 멤버만 초대할 수 있습니다");
          }
      
          String token = jwtUtil.generateInviteToken(familyId);

          FamilyInvite invite = FamilyInvite.builder()
                  .familyId(familyId)
                  .token(token)
                  .inviterId(inviterId)
                  .build();
          FamilyInvite saved = familyInviteRepository.save(invite);

          return new CreateInviteResponse(token, saved.getExpiresAt());
      }
   // 초대 수락: 토큰 검증 → 1회용/만료/중복 체크 → 멤버 추가 + 초대 사용 처리
      @Transactional
      public AcceptInviteResponse acceptInvite(AcceptInviteRequest request, Long memberId) {
          Long familyId = jwtUtil.getFamilyIdFromInviteToken(request.token());

          FamilyInvite invite = familyInviteRepository.findByToken(request.token())
                  .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 초대입니다"));

          if (invite.isExpired()) {
              throw new IllegalArgumentException("만료된 초대입니다");
          }
          if (invite.isUsed()) {
              throw new IllegalArgumentException("이미 사용된 초대입니다");
          }
          if (familyMemberRepository.existsByFamilyIdAndMemberId(familyId, memberId)) {
              throw new IllegalArgumentException("이미 가족 멤버입니다");
          }

          Family family = familyRepository.findById(familyId)
                  .orElseThrow(() -> new IllegalArgumentException("가족이 존재하지 않습니다"));

          FamilyMember newMember = FamilyMember.builder()
                  .familyId(familyId)
                  .memberId(memberId)
                  .role("MEMBER")
                  .build();
          familyMemberRepository.save(newMember);

          invite.markAsUsed(memberId);  // JPA dirty checking이 자동 UPDATE

          return new AcceptInviteResponse(family.getId(), family.getName());
      }
      
      
  }