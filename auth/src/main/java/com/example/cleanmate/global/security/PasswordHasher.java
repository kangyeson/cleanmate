package com.example.cleanmate.global.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public class PasswordHasher {

    private final SecureRandom random = new SecureRandom();

    /** 랜덤 16바이트 hex salt 생성 */
    public String generateSalt() {
        byte[] bytes = new byte[16];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** SHA-256(password + salt) → hex */
    public String hash(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] result = digest.digest((password + salt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(result);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean matches(String password, String salt, String storedHash) {
        return hash(password, salt).equals(storedHash);
    }
}
