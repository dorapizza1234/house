 package com.example.house.controller;

  import com.example.house.dto.LoginRequest;
import com.example.house.dto.LoginResponse;
import com.example.house.dto.SignupRequest;
  import com.example.house.dto.SignupResponse;
  import com.example.house.service.AuthService;
  import jakarta.validation.Valid;
  import lombok.RequiredArgsConstructor;
  import org.springframework.http.HttpStatus;
  import org.springframework.http.ResponseEntity;
  import org.springframework.web.bind.annotation.PostMapping;
  import org.springframework.web.bind.annotation.RequestBody;
  import org.springframework.web.bind.annotation.RequestMapping;
  import org.springframework.web.bind.annotation.RestController;

  @RestController
  @RequestMapping("/api/auth")
  @RequiredArgsConstructor
  public class AuthController {

      private final AuthService authService;
       // signup
      @PostMapping("/signup")
      public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
          SignupResponse response = authService.signup(request);
          return ResponseEntity.status(HttpStatus.CREATED).body(response);
      }
      
      @PostMapping("/login")
      public ResponseEntity<LoginResponse>login(@Valid @RequestBody LoginRequest req){
    	 LoginResponse res=authService.login(req);
    	 return ResponseEntity.ok(res);
      }
      
      
      
  }