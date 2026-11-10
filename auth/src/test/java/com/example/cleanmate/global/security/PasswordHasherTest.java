package com.example.cleanmate.global.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void matches_성공() {
        String hash = hasher.hash("1234");
        assertTrue(hasher.matches("1234", hash));
    }

    @Test
    void matches_실패_다른_비밀번호() {
        String hash = hasher.hash("1234");
        assertFalse(hasher.matches("wrong", hash));
    }

    @Test
    void 같은_비밀번호를_두_번_hash하면_결과가_다름() {
        // BCrypt는 매번 새 salt를 생성
        assertNotEquals(hasher.hash("1234"), hasher.hash("1234"));
    }

    @Test
    void BCrypt_형식_확인() {
        String hash = hasher.hash("1234");
        assertTrue(hash.startsWith("$2a$"), "BCrypt 해시는 $2a$로 시작해야 함");
        assertEquals(60, hash.length(), "BCrypt 해시는 60자여야 함");
    }
}
