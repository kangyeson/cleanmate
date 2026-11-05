package com.example.cleanmate.global.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher();

    // ── 결정성 ────────────────────────────────────────────────

    @Test
    void 같은_입력과_같은_salt는_같은_해시() {
        String salt = "fixed-salt";
        assertEquals(hasher.hash("password", salt), hasher.hash("password", salt));
    }

    @Test
    void 비밀번호가_달라도_salt_같으면_해시가_다름() {
        String salt = "fixed-salt";
        assertNotEquals(hasher.hash("abc", salt), hasher.hash("xyz", salt));
    }

    // ── salt 효과 ────────────────────────────────────────────

    @Test
    void 같은_입력이라도_salt가_다르면_해시가_다름() {
        assertNotEquals(
                hasher.hash("1234", "salt-a"),
                hasher.hash("1234", "salt-b")
        );
    }

    // ── matches ──────────────────────────────────────────────

    @Test
    void matches_성공() {
        String salt = hasher.generateSalt();
        String hash = hasher.hash("1234", salt);
        assertTrue(hasher.matches("1234", salt, hash));
    }

    @Test
    void matches_실패_다른_비밀번호() {
        String salt = hasher.generateSalt();
        String hash = hasher.hash("1234", salt);
        assertFalse(hasher.matches("wrong", salt, hash));
    }

    @Test
    void matches_실패_다른_salt() {
        String salt = hasher.generateSalt();
        String hash = hasher.hash("1234", salt);
        assertFalse(hasher.matches("1234", "other-salt", hash));
    }

    // ── generateSalt ─────────────────────────────────────────

    @Test
    void generateSalt_32자_16진수() {
        String salt = hasher.generateSalt();
        assertEquals(32, salt.length());
        assertTrue(salt.matches("[0-9a-f]{32}"));
    }
}
