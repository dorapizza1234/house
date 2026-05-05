 package com.example.house.repository;

  import com.example.house.domain.FamilyMember;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.util.List;

  public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

      boolean existsByFamilyIdAndMemberId(Long familyId, Long memberId);

      List<FamilyMember> findByMemberId(Long memberId);
      
      List<FamilyMember> findByFamilyId(Long familyId);
  }