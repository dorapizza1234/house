
  package com.example.house.repository;

  import com.example.house.domain.Message;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.time.LocalDateTime;
  import java.util.List;

  public interface MessageRepository extends JpaRepository<Message, Long> {

      // 가족 메시지 이력 (최신순)
      List<Message> findByFamilyIdOrderByCreatedAtDesc(Long familyId);

      // 본인이 오늘 보낸 프리셋 메시지 카운트 (하루 3회 제한 검증용)
      long countBySenderIdAndMessageTypeIdIsNotNullAndCreatedAtBetween(
              Long senderId, LocalDateTime start, LocalDateTime end);
  }
