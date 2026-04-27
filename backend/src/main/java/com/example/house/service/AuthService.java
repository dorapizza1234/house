package com.example.house.service;

  import com.example.house.domain.Member;
  import com.example.house.dto.SignupRequest;
  import com.example.house.dto.SignupResponse;
  import com.example.house.repository.MemberRepository;
  import lombok.RequiredArgsConstructor;
  import org.springframework.security.crypto.password.PasswordEncoder;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  @Service
  @RequiredArgsConstructor
  public class AuthService {

      private final MemberRepository memberRepository;
      private final PasswordEncoder passwordEncoder;

      @Transactional
      public SignupResponse signup(SignupRequest request) {
          if (memberRepository.existsByEmail(request.email())) {
              throw new IllegalArgumentException("이미 가입된 이메일입니다");
          }

          String hashedPassword = passwordEncoder.encode(request.password());

          Member member = Member.builder()
                  .email(request.email())
                  .passwordHash(hashedPassword)
                  .nickname(request.nickname())
                  .build();

          Member saved = memberRepository.save(member);

          return new SignupResponse(
                  saved.getId(),
                  saved.getEmail(),
                  saved.getNickname()
          );
      }
  }