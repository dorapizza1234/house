package com.example.house.repository;

  import com.example.house.domain.PasswordResetToken;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.util.Optional;

  public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

      Optional<PasswordResetToken> findByToken(String token);
  }