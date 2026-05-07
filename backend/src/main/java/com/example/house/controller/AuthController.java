package com.example.house.controller;

import com.example.house.dto.EmailCheckResponse;
import com.example.house.dto.FindIdRequest;
import com.example.house.dto.FindIdResponse;
import com.example.house.dto.LoginRequest;
import com.example.house.dto.LoginResponse;
import com.example.house.dto.PasswordResetConfirmRequest;
import com.example.house.dto.PasswordResetRequestDto;
import com.example.house.dto.SendCodeRequest;
import com.example.house.dto.SignupRequest;
import com.example.house.dto.SignupResponse;
import com.example.house.dto.VerifyCodeRequest;
import com.example.house.dto.VerifyCodeResponse;
import com.example.house.service.AuthService;
import com.example.house.service.PasswordResetService;
import com.example.house.service.PhoneVerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        LoginResponse res = authService.login(req);
        return ResponseEntity.ok(res);
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
