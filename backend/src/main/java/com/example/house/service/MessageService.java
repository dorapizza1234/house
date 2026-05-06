package com.example.house.service;

import com.example.house.domain.FamilyEvent;
import com.example.house.domain.FamilyMember;
import com.example.house.domain.Member;
import com.example.house.domain.Message;
import com.example.house.domain.MessageType;
import com.example.house.domain.SpecialHoliday;
import com.example.house.dto.MessageResponse;
import com.example.house.dto.SendMessageRequest;
import com.example.house.dto.SendMessageResponse;
import com.example.house.repository.FamilyEventRepository;
import com.example.house.repository.FamilyMemberRepository;
import com.example.house.repository.MemberRepository;
import com.example.house.repository.MessageRepository;
import com.example.house.repository.MessageTypeRepository;
import com.example.house.repository.SpecialHolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MessageService {

    private static final int DAILY_PRESET_LIMIT = 3;
    private static final int PRESET_BASE_POINTS = 10;
    private static final int FREE_BASE_POINTS = 0;

    private final MessageRepository messageRepository;
    private final MessageTypeRepository messageTypeRepository;
    private final SpecialHolidayRepository specialHolidayRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final FamilyEventRepository familyEventRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public SendMessageResponse sendMessage(Long senderId, SendMessageRequest request) {
        FamilyMember senderFamilyMember = familyMemberRepository.findByMemberId(senderId)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("가족이 없습니다"));
        Long familyId = senderFamilyMember.getFamilyId();

        if (!familyMemberRepository.existsByFamilyIdAndMemberId(familyId, request.receiverId())) {
            throw new IllegalArgumentException("같은 가족 멤버가 아닙니다");
        }

        boolean isPreset = request.messageTypeId() != null;
        long todayCountBefore = countTodayPreset(senderId);

        if (isPreset) {
            if (!messageTypeRepository.existsById(request.messageTypeId())) {
                throw new IllegalArgumentException("유효하지 않은 메시지 종류입니다");
            }
            if (todayCountBefore >= DAILY_PRESET_LIMIT) {
                throw new IllegalArgumentException(
                        "오늘 프리셋 메시지 한도를 초과했습니다 (최대 " + DAILY_PRESET_LIMIT + "회)");
            }
        }

        int basePoints = isPreset ? PRESET_BASE_POINTS : FREE_BASE_POINTS;
        EventBonus bonus = calculateEventBonus(familyId);
        int finalPoints = basePoints * bonus.multiplier();

        Message message = Message.builder()
                .familyId(familyId)
                .senderId(senderId)
                .receiverId(request.receiverId())
                .messageTypeId(request.messageTypeId())
                .content(request.content())
                .basePoints(basePoints)
                .multiplier(bonus.multiplier())
                .finalPoints(finalPoints)
                .eventReasons(bonus.reasons())
                .build();
        messageRepository.save(message);

        senderFamilyMember.addPoints(finalPoints);

        int todayCountAfter = (int) todayCountBefore + (isPreset ? 1 : 0);
        return new SendMessageResponse(
                message.getId(),
                finalPoints,
                bonus.multiplier(),
                bonus.reasons(),
                todayCountAfter);
    }

    public List<MessageResponse> getFamilyMessages(Long familyId, Long viewerId) {
        if (!familyMemberRepository.existsByFamilyIdAndMemberId(familyId, viewerId)) {
            throw new IllegalArgumentException("가족 멤버가 아닙니다");
        }

        List<Message> messages = messageRepository.findByFamilyIdOrderByCreatedAtDesc(familyId);
        if (messages.isEmpty()) {
            return List.of();
        }

        Set<Long> memberIds = messages.stream()
                .flatMap(m -> Stream.of(m.getSenderId(), m.getReceiverId()))
                .collect(Collectors.toSet());
        Map<Long, String> nicknameMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(Member::getId, Member::getNickname));

        Set<Long> typeIds = messages.stream()
                .map(Message::getMessageTypeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> typeNameMap = typeIds.isEmpty()
                ? Map.of()
                : messageTypeRepository.findAllById(typeIds).stream()
                        .collect(Collectors.toMap(MessageType::getId, MessageType::getNameKo));

        return messages.stream()
                .map(m -> new MessageResponse(
                        m.getId(),
                        m.getSenderId(),
                        nicknameMap.get(m.getSenderId()),
                        m.getReceiverId(),
                        nicknameMap.get(m.getReceiverId()),
                        m.getMessageTypeId(),
                        m.getMessageTypeId() != null ? typeNameMap.get(m.getMessageTypeId()) : null,
                        m.getContent(),
                        m.getFinalPoints(),
                        m.getMultiplier(),
                        m.getEventReasons(),
                        m.getCreatedAt()))
                .toList();
    }

    private long countTodayPreset(Long senderId) {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        return messageRepository.countBySenderIdAndMessageTypeIdIsNotNullAndCreatedAtBetween(
                senderId, start, end);
    }

    private EventBonus calculateEventBonus(Long familyId) {
        LocalDate today = LocalDate.now();
        List<String> reasons = new ArrayList<>();

        List<SpecialHoliday> holidays = specialHolidayRepository
                .findByMonthAndDay(today.getMonthValue(), today.getDayOfMonth());
        for (SpecialHoliday h : holidays) {
            reasons.add(h.getNameKo());
        }

        List<FamilyEvent> events = familyEventRepository.findByFamilyId(familyId);
        for (FamilyEvent e : events) {
            if (e.isYearly()
                    && e.getEventDate().getMonthValue() == today.getMonthValue()
                    && e.getEventDate().getDayOfMonth() == today.getDayOfMonth()) {
                reasons.add(e.getTitle());
            }
        }

        int multiplier = (int) Math.pow(2, reasons.size());
        String reasonsStr = reasons.isEmpty() ? null : String.join(",", reasons);

        return new EventBonus(multiplier, reasonsStr);
    }

    private record EventBonus(int multiplier, String reasons) {}
}
