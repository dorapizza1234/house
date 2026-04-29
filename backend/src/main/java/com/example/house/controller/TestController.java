package com.example.house.controller;

  import org.springframework.security.core.annotation.AuthenticationPrincipal;
  import org.springframework.web.bind.annotation.GetMapping;
  import org.springframework.web.bind.annotation.RequestMapping;
  import org.springframework.web.bind.annotation.RestController;

  @RestController
  @RequestMapping("/api/test")
  public class TestController {

      @GetMapping("/me")
      public String me(@AuthenticationPrincipal String email) {
          return "인증된 사용자: " + email;
      }
  }
