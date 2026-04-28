package com.example.house.service;

  import com.example.house.domain.Member;
import com.example.house.dto.LoginRequest;
import com.example.house.dto.LoginResponse;
import com.example.house.dto.SignupRequest;
  import com.example.house.dto.SignupResponse;
  import com.example.house.repository.MemberRepository;
import com.example.house.security.JwtUtil;

import lombok.RequiredArgsConstructor;
  import org.springframework.security.crypto.password.PasswordEncoder;
  import org.springframework.stereotype.Service;
  import org.springframework.transaction.annotation.Transactional;

  @Service
  @RequiredArgsConstructor
  public class AuthService {

      private final MemberRepository memberRepository;
      private final PasswordEncoder passwordEncoder;
      private final JwtUtil jwtUtil;
      
      
      //이메일 중복 체크 + 비번 해싱 + DB 저장
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
                  .birthDate(request.birthDate())
                  .build();

          Member saved = memberRepository.save(member);

          return new SignupResponse(
                  saved.getId(),
                  saved.getEmail(),
                  saved.getNickname()
          );
      }
      
      //회원 조회 + 비번 매칭 + 토큰 발급
      @Transactional(readOnly = true)
      public LoginResponse login(LoginRequest request) {
          Member member = memberRepository.findByEmail(request.email())
                  .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다"));

          if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
              throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다");
          }

          String accessToken = jwtUtil.generateAccessToken(member.getEmail());
          String refreshToken = jwtUtil.generateRefreshToken(member.getEmail());

          return new LoginResponse(accessToken, refreshToken);
      }
      
      
  }