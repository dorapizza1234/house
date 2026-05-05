  package com.example.house.repository;

  import com.example.house.domain.FamilyEvent;
  import org.springframework.data.jpa.repository.JpaRepository;

  import java.util.List;

  public interface FamilyEventRepository extends JpaRepository<FamilyEvent, Long> {                                     
      List<FamilyEvent> findByFamilyId(Long familyId);                                                                    
      
  
  }