package com.example.house.repository;

  import com.example.house.domain.Member;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.util.Optional;

  public interface MemberRepository extends JpaRepository<Member, Long> {

      Optional<Member> findByEmail(String email);

      boolean existsByEmail(String email);
      
      Optional<Member> findByPhone(String phone);

      boolean existsByPhone(String phone); 
      
  }
