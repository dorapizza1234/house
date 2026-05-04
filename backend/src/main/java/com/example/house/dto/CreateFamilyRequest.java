 package com.example.house.dto;

  import jakarta.validation.constraints.NotBlank;
  import jakarta.validation.constraints.Size;

  public record CreateFamilyRequest(
      @NotBlank(message = "가족 이름은 필수입니다")
      @Size(max = 30, message = "가족 이름은 30자 이하여야 합니다")
      String name
  ) {}