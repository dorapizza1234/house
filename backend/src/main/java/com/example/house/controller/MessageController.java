package com.example.house.controller;

import com.example.house.domain.Member;
import com.example.house.dto.MessageResponse;
import com.example.house.dto.SendMessageRequest;
import com.example.house.dto.SendMessageResponse;
import com.example.house.repository.MemberRepository;
import com.example.house.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final MemberRepository memberRepository;

    @PostMapping("/api/messages")
    public ResponseEntity<SendMessageResponse> sendMessage(
            @Valid @RequestBody SendMessageRequest request,
            @AuthenticationPrincipal String email
    ) {
        Long senderId = resolveMemberId(email);
        SendMessageResponse response = messageService.sendMessage(senderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/families/{familyId}/messages")
    public ResponseEntity<List<MessageResponse>> getFamilyMessages(
            @PathVariable("familyId") Long familyId,
            @AuthenticationPrincipal String email
    ) {
        Long viewerId = resolveMemberId(email);
        List<MessageResponse> response = messageService.getFamilyMessages(familyId, viewerId);
        return ResponseEntity.ok(response);
    }

    private Long resolveMemberId(String email) {
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("회원이 존재하지 않습니다"));
        return member.getId();
    }
}
