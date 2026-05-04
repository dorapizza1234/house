package com.example.house.repository;

  import com.example.house.domain.Family;
  import org.springframework.data.jpa.repository.JpaRepository;

  public interface FamilyRepository extends JpaRepository<Family, Long> {
  }
