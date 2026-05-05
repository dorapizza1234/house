package com.example.house.controller;
import com.example.house.domain.Member;
import com.example.house.dto.PresenceResponse;
import com.example.house.repository.MemberRepository;
import com.example.house.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/presence")
@RequiredArgsConstructor
public class PresenceController {

    private final MemberService memberService;
    private final MemberRepository memberRepository;

    @PostMapping("/toggle")
    public ResponseEntity<PresenceResponse> toggle(@AuthenticationPrincipal String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

        PresenceResponse response = memberService.togglePresence(member.getId());
        return ResponseEntity.ok(response);
    }
}

