 package com.example.house.service;

  import com.example.house.domain.Member;
import com.example.house.dto.PresenceResponse;
import com.example.house.repository.MemberRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  public class MemberService {

      private final MemberRepository memberRepository;

      @Transactional
      public PresenceResponse togglePresence(Long memberId) {
          Member member = memberRepository.findById(memberId)
                  .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));                              
          member.togglePresence();

          return new PresenceResponse(member.getPresenceStatus(), member.getPresenceUpdatedAt());
      }
  }
