package com.example.house;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 테스트 데이터용 BCrypt 해시 생성.
 * 사용법: 우클릭 → Run As → JUnit Test → 콘솔에 출력된 HASH = ... 복사
 */
class HashGenTest {

    @Test
    void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "Qwer1234!";
        String hash = encoder.encode(password);
        System.out.println("====================================");
        System.out.println("PASSWORD = " + password);
        System.out.println("HASH     = " + hash);
        System.out.println("====================================");
    }
}
