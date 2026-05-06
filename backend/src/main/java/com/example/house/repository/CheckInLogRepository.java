package com.example.house.repository;

  import com.example.house.domain.CheckInLog;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.time.LocalDateTime;
  import java.util.List;

  public interface CheckInLogRepository extends JpaRepository<CheckInLog, Long> {

      List<CheckInLog> findByMemberIdAndCheckedAtAfterOrderByCheckedAtDesc(
              Long memberId, LocalDateTime after);

      List<CheckInLog> findByFamilyIdAndCheckedAtAfterOrderByCheckedAtDesc(
              Long familyId, LocalDateTime after);
  }