package com.example.house.security;

  import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
  import org.springframework.beans.factory.annotation.Value;
  import org.springframework.stereotype.Component;

  import javax.crypto.SecretKey;
  import java.nio.charset.StandardCharsets;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import java.util.Date;

  @Component
  public class JwtUtil {

      private final SecretKey key;
      private final long accessExpiration;
      private final long refreshExpiration;
      private final long inviteExpiration;

      public JwtUtil(
              @Value("${jwt.secret}") String secret,
              @Value("${jwt.access-expiration}") long accessExpiration,
              @Value("${jwt.refresh-expiration}") long refreshExpiration,
              @Value("${jwt.invite-expiration}") long inviteExpiration
      ) {
          this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
          this.accessExpiration = accessExpiration;
          this.refreshExpiration = refreshExpiration;
          this.inviteExpiration = inviteExpiration;
      }
 
  
  public String generateAccessToken(String email) {
      Date now = new Date();
      Date expiry = new Date(now.getTime() + accessExpiration);
      return Jwts.builder()
              .subject(email)
              .issuedAt(now)
              .expiration(expiry)
              .signWith(key)
              .compact();
  }

  public String generateRefreshToken(String email) {
      Date now = new Date();
      Date expiry = new Date(now.getTime() + refreshExpiration);
      return Jwts.builder()
              .subject(email)
              .issuedAt(now)
              .expiration(expiry)
              .signWith(key)
              .compact();
  }

  public String getEmailFromToken(String token) {
      return Jwts.parser()
              .verifyWith(key)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject();
  }

  public boolean validateToken(String token) {
      try {
          Jwts.parser()
                  .verifyWith(key)
                  .build()
                  .parseSignedClaims(token);
          return true;
      } catch (JwtException | IllegalArgumentException e) {
          return false;
      }
  }
  
  public String generateInviteToken(Long familyId) {
      Date now = new Date();
      Date expiry = new Date(now.getTime() + inviteExpiration);
      return Jwts.builder()
              .subject(String.valueOf(familyId))
              .claim("type", "invite")
              .issuedAt(now)
              .expiration(expiry)
              .signWith(key)
              .compact();
  }

  public Long getFamilyIdFromInviteToken(String token) {
      String subject = Jwts.parser()
              .verifyWith(key)
              .build()
              .parseSignedClaims(token)
              .getPayload()
              .getSubject();
      return Long.parseLong(subject);
  }

  public String generatePhoneVerificationToken(String phone, String purpose) {
      Date now = new Date();
      Date expiry = new Date(now.getTime() + 10 * 60 * 1000L); // 10분

      return Jwts.builder()
              .subject(phone)
              .claim("type", "phone_verification")
              .claim("purpose", purpose)
              .issuedAt(now)
              .expiration(expiry)
              .signWith(key)
              .compact();
  }

  public PhoneVerificationClaim parsePhoneVerificationToken(String token) {
      Claims claims = Jwts.parser()
              .verifyWith(key)
              .build()
              .parseSignedClaims(token)
              .getPayload();

      String type = claims.get("type", String.class);
      if (!"phone_verification".equals(type)) {
          throw new IllegalArgumentException("유효하지 않은 토큰 타입입니다");
      }

      return new PhoneVerificationClaim(
              claims.getSubject(),
              claims.get("purpose", String.class)
      );
  }

  public record PhoneVerificationClaim(String phone, String purpose) {}
  
  
  
  }
  