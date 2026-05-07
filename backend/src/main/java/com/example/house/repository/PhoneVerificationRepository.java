 package com.example.house.repository;

  import com.example.house.domain.PhoneVerification;
  import org.springframework.data.jpa.repository.JpaRepository;
  import org.springframework.data.jpa.repository.Modifying;
  import org.springframework.data.jpa.repository.Query;
  import org.springframework.data.repository.query.Param;

  import java.time.LocalDateTime;
  import java.util.Optional;

  public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification, Long> {

      // 가장 최근 발송된 코드 조회 (verifyCode에서 사용)
      Optional<PhoneVerification> findTopByPhoneAndPurposeOrderByCreatedAtDesc(
              String phone, String purpose
      );

      // Rate limiting용: 최근 N초 내 발송 이력 있는지
      boolean existsByPhoneAndCreatedAtAfter(String phone, LocalDateTime since);

      // 새 코드 발급 시 같은 phone+purpose의 미검증 코드 모두 만료 처리 (UPDATE 쿼리)
      @Modifying
      @Query("UPDATE PhoneVerification pv " +
             "SET pv.expiresAt = :now " +
             "WHERE pv.phone = :phone " +
             "AND pv.purpose = :purpose " +
             "AND pv.verifiedAt IS NULL " +
             "AND pv.expiresAt > :now")
      void invalidateActiveCodes(
              @Param("phone") String phone,
              @Param("purpose") String purpose,
              @Param("now") LocalDateTime now
      );
  }
