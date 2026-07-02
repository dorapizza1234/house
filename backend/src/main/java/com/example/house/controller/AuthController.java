package com.example.house.controller;

import com.example.house.dto.EmailCheckResponse;
import com.example.house.dto.FindIdRequest;
import com.example.house.dto.FindIdResponse;
import com.example.house.dto.LoginRequest;
import com.example.house.dto.LoginResponse;
import com.example.house.dto.PasswordResetConfirmRequest;
import com.example.house.dto.PasswordResetRequestDto;
import com.example.house.dto.RefreshResponse;                
import com.example.house.dto.SendCodeRequest;
import com.example.house.dto.SignupRequest;
import com.example.house.dto.SignupResponse;
import com.example.house.dto.VerifyCodeRequest;
import com.example.house.dto.VerifyCodeResponse;
import com.example.house.security.JwtUtil;
import com.example.house.security.JwtUtil.RefreshClaim;
import com.example.house.service.AuthService;
import com.example.house.service.PasswordResetService;
import com.example.house.service.PhoneVerificationService;
import com.example.house.service.RefreshTokenService;         
import io.jsonwebtoken.JwtException;                         
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;                 
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;              
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;  
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final PhoneVerificationService phoneVerificationService;
    private final PasswordResetService passwordResetService;
    private final RefreshTokenService refreshTokenService;  
    private final JwtUtil jwtUtil;
    
    private static final String REFRESH_COOKIE = "refreshToken";
    private static final long REFRESH_MAX_AGE = 7L * 24 * 60 * 60;   // 7일(초)
    
    private ResponseCookie buildRefreshCookie(String token, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }
    
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse res = authService.login(req);
        ResponseCookie cookie = buildRefreshCookie(res.refreshToken(), REFRESH_MAX_AGE);
        // 바디에선 refresh 빼기 (null → JsonInclude로 제외)
        LoginResponse body = new LoginResponse(res.accessToken(), null, res.familyId());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(body);
    }
    // 쿠키의 refresh로 회전 → 새 access(바디) + 새 refresh(쿠키)
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken == null) {
            throw new JwtException("리프레시 토큰이 없습니다.");
        }
        RefreshTokenService.TokenPair pair = refreshTokenService.rotate(refreshToken);
        ResponseCookie cookie = buildRefreshCookie(pair.refreshToken(), REFRESH_MAX_AGE);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new RefreshResponse(pair.accessToken()));
    }

    // 로그아웃 — Redis jti 삭제 + 쿠키 만료
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        if (refreshToken != null) {
            try {
                RefreshClaim claim = jwtUtil.parseRefreshToken(refreshToken);
                refreshTokenService.logout(claim.email());
            } catch (JwtException ignored) {
                // 이미 무효한 토큰이면 그냥 쿠키만 지움
            }
        }
        ResponseCookie cleared = buildRefreshCookie("", 0);   // maxAge 0 = 즉시 만료
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleared.toString())
                .build();
    }
    @GetMapping("/email/check")
    public ResponseEntity<EmailCheckResponse> checkEmail(@RequestParam("email") String email) {
        boolean available = authService.isEmailAvailable(email);
        return ResponseEntity.ok(new EmailCheckResponse(available));
    }

    @PostMapping("/phone/send-code")
    public ResponseEntity<Void> sendPhoneCode(@Valid @RequestBody SendCodeRequest request) {
        phoneVerificationService.sendCode(request.phone(), request.purpose());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/phone/verify-code")
    public ResponseEntity<VerifyCodeResponse> verifyPhoneCode(@Valid @RequestBody VerifyCodeRequest request) {
        String token = phoneVerificationService.verifyCode(
                request.phone(), request.code(), request.purpose()
        );
        return ResponseEntity.ok(new VerifyCodeResponse(token));
    }

    @PostMapping("/find-id")
    public ResponseEntity<FindIdResponse> findId(@Valid @RequestBody FindIdRequest request) {
        return ResponseEntity.ok(authService.findId(request.verificationToken()));
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDto request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok().build();   // 이메일 존재 여부 노출 X
    }

    @PostMapping("/password/reset-confirm")
    public ResponseEntity<Void> confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }
}
