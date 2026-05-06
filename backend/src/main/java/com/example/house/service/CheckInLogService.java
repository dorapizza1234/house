 package com.example.house.service;

  import com.example.house.domain.CheckInLog;
  import com.example.house.domain.Member;                                                                                 import com.example.house.dto.CheckInLogResponse;
  import com.example.house.repository.CheckInLogRepository;                                                               import com.example.house.repository.FamilyMemberRepository;
  import com.example.house.repository.MemberRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;
  import java.util.Map;
  import java.util.stream.Collectors;
  import java.time.LocalDateTime;
  import java.util.List;

  @Service
  @RequiredArgsConstructor
  @Transactional(readOnly = true)
  public class CheckInLogService {

      private static final int MAX_DAYS = 30;

      private final CheckInLogRepository checkInLogRepository;
      private final MemberRepository memberRepository;
      private final FamilyMemberRepository familyMemberRepository;

      public List<CheckInLogResponse> getMyLogs(Long memberId, int days) {
          validateDays(days);

          Member member = memberRepository.findById(memberId)
                  .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다"));

          LocalDateTime after = LocalDateTime.now().minusDays(days);
          List<CheckInLog> logs = checkInLogRepository
                  .findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(memberId, after);

          return logs.stream()
                  .map(log -> new CheckInLogResponse(
                          log.getId(),
                          log.getMemberId(),
                          member.getNickname(),
                          log.getStatus(),
                          log.getCheckedAt()))
                  .toList();
      }

      private void validateDays(int days) {
          if (days < 1 || days > MAX_DAYS) {
              throw new IllegalArgumentException("days는 1~" + MAX_DAYS + " 사이여야 합니다");
          }
      }
      
      
      public List<CheckInLogResponse> getFamilyLogs(Long familyId, Long memberId, int days) {
          validateDays(days);                                                                                               
          if (!familyMemberRepository.existsByFamilyIdAndMemberId(familyId, memberId)) {
              throw new IllegalArgumentException("가족 멤버가 아닙니다");
          }

          LocalDateTime after = LocalDateTime.now().minusDays(days);
          List<CheckInLog> logs = checkInLogRepository
                  .findByFamilyIdAndCheckedAtAfterOrderByCheckedAtDesc(familyId, after);

          List<Long> memberIds = logs.stream()
                  .map(CheckInLog::getMemberId)
                  .distinct()
                  .toList();

          Map<Long, String> nicknameMap = memberRepository.findAllById(memberIds).stream()
                  .collect(Collectors.toMap(Member::getId, Member::getNickname));

          return logs.stream()
                  .map(log -> new CheckInLogResponse(
                          log.getId(),
                          log.getMemberId(),
                          nicknameMap.get(log.getMemberId()),
                          log.getStatus(),
                          log.getCheckedAt()))
                  .toList();
      }
      
      
      
      
      
      
      
      
      
      
      
      
      
  }
