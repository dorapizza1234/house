package com.example.house.controller;

import com.example.house.care.ChatService;
import com.example.house.domain.Member;
import com.example.house.dto.ChatRequest;
import com.example.house.dto.ChatResponse;
import com.example.house.repository.FamilyMemberRepository;
import com.example.house.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final MemberRepository memberRepository;
    private final FamilyMemberRepository familyMemberRepository;

    @PostMapping("/api/chat")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal String email
    ) {
        Long familyId = resolveFamilyId(email);   // 서버가 familyId 결정 — 클라/AI가 못 넘김
        String reply = chatService.chat(familyId, request.message());
        return ResponseEntity.ok(new ChatResponse(reply));
    }

    private Long resolveFamilyId(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다"));
        return familyMemberRepository.findByMemberId(member.getId())
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("가족이 없습니다"))
                .getFamilyId();
    }
}
