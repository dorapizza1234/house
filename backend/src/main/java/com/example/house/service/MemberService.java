package com.example.house.service;

import com.example.house.domain.CheckInLog;
import com.example.house.domain.FamilyMember;
import com.example.house.domain.Member;
import com.example.house.dto.PresenceResponse;
import com.example.house.repository.CheckInLogRepository;
import com.example.house.repository.FamilyMemberRepository;
import com.example.house.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final CheckInLogRepository checkInLogRepository;

    @Transactional
    public PresenceResponse togglePresence(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        member.togglePresence();

        Long familyId = familyMemberRepository.findByMemberId(memberId).stream()
                .findFirst()
                .map(FamilyMember::getFamilyId)
                .orElse(null);

        CheckInLog log = CheckInLog.builder()
                .memberId(memberId)
                .familyId(familyId)
                .status(member.getPresenceStatus())
                .checkedAt(member.getPresenceUpdatedAt())
                .build();
        checkInLogRepository.save(log);

        return new PresenceResponse(member.getPresenceStatus(), member.getPresenceUpdatedAt());
    }
}
