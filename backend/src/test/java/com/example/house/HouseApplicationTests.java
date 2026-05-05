package com.example.house;

  import org.junit.jupiter.api.Disabled;
  import org.junit.jupiter.api.Test;
  import org.springframework.boot.test.context.SpringBootTest;

  @Disabled("env vars not configured for test runner")
  @SpringBootTest
  class HouseApplicationTests {

      @Test
      void contextLoads() {
      }
}