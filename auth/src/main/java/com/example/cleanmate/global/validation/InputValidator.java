package com.example.cleanmate.global.validation;

import com.example.cleanmate.global.exception.CleanmateException;

/**
 * 방 생성·비밀번호 변경에서 재사용하는 입력 검증 유틸리티.
 * Spring MVC 없이 서비스 레벨에서 직접 호출한다.
 */
public final class InputValidator {

    private InputValidator() {}

    /**
     * 이름 검증: trim 후 1~30자.
     * @throws CleanmateException 400 조건 불충족 시
     */
    public static String validateName(String name) {
        if (name == null) {
            throw new CleanmateException("이름은 필수입니다.", 400);
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 30) {
            throw new CleanmateException("이름은 1~30자여야 합니다.", 400);
        }
        return trimmed;
    }

    /**
     * 비밀번호 검증: 4~50자.
     * bcrypt는 72바이트까지만 반영하므로 상한을 둔다.
     * @throws CleanmateException 400 조건 불충족 시
     */
    public static void validatePassword(String password) {
        if (password == null || password.length() < 4) {
            throw new CleanmateException("비밀번호는 4자 이상이어야 합니다.", 400);
        }
        if (password.length() > 50) {
            throw new CleanmateException("비밀번호는 50자 이하여야 합니다.", 400);
        }
    }
}
