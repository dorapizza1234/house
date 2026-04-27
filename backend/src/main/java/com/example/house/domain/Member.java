package com.example.house.domain;

  import jakarta.persistence.*;
  import lombok.*;
  import java.time.LocalDateTime;

  @Entity
  @Table(name = "개인")
  @Getter
  @NoArgsConstructor(access = AccessLevel.PROTECTED)
  public class Member {

      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "개인아이디")
      private Long id;

      @Column(name = "이메일", nullable = false, unique = true)
      private String email;

      @Column(name = "비밀번호해시", nullable = false)
      private String passwordHash;

      @Column(name = "닉네임", nullable = false)
      private String nickname;

      @Column(name = "가입시간")
      private LocalDateTime joinedAt;

      @Builder
      public Member(String email, String passwordHash, String nickname) {
          this.email = email;
          this.passwordHash = passwordHash;
          this.nickname = nickname;
          this.joinedAt = LocalDateTime.now();
      }
  }