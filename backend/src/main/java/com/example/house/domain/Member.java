package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

  @Entity
  @Table(name = "member")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class Member {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "id")
      private Long id;

      @Column(name = "email", nullable = false, unique = true)
      private String email;
      
      @Column(name="phone",nullable= false ,unique=true)
      private String phone;
      
      @Column(name = "password_hash", nullable = false)
      private String passwordHash;

      @Column(name = "nickname", nullable = false)
      private String nickname;

      @Column(name = "joined_at")
      private LocalDateTime joinedAt;

      @Column(name = "birth_date")
      private LocalDate birthDate;

      @Column(name = "presence_status", nullable = false)
      private String presenceStatus;

      @Column(name = "presence_updated_at", nullable = false)
      private LocalDateTime presenceUpdatedAt;
      
      @Builder
      public Member(String email, String passwordHash, String nickname, LocalDate birthDate, String phone) {
          this.email = email;
          this.passwordHash = passwordHash;
          this.nickname = nickname;
          this.birthDate = birthDate;
          this.phone = phone;
          this.joinedAt = LocalDateTime.now();
          this.presenceStatus = "OUTSIDE";
          this.presenceUpdatedAt = LocalDateTime.now();
      }
      
      public void togglePresence() {
          this.presenceStatus = "HOME".equals(this.presenceStatus) ? "OUTSIDE" : "HOME";
          this.presenceUpdatedAt = LocalDateTime.now();
      }
      
      public void updatePassword(String newPasswordHash) {
          this.passwordHash = newPasswordHash;
      }
      
  }