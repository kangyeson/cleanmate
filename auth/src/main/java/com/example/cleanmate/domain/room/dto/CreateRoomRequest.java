package com.example.cleanmate.domain.room.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * POST /api/rooms 요청 본문.
 */
public record CreateRoomRequest(

        @NotBlank(message = "방 이름은 필수입니다.")
        String roomName,

        @NotBlank(message = "관리자 이름은 필수입니다.")
        String adminName,

        @NotEmpty(message = "룸메이트는 1명 이상이어야 합니다.")
        @Valid
        List<MemberInput> members,

        @NotEmpty(message = "구역은 1개 이상이어야 합니다.")
        List<String> zones
) {

    public record MemberInput(
            @NotBlank(message = "멤버 이름은 필수입니다.")
            String name,

            @NotBlank(message = "비밀번호는 필수입니다.")
            String password
    ) {}
}
