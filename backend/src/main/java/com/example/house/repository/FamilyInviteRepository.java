package com.example.house.repository;

import com.example.house.domain.FamilyInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FamilyInviteRepository extends JpaRepository<FamilyInvite, Long> {

    Optional<FamilyInvite> findByToken(String token);
}