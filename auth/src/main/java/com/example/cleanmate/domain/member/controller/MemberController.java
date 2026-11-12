package com.example.cleanmate.domain.member.controller;

import com.example.cleanmate.domain.member.dto.AddMemberRequest;
import com.example.cleanmate.domain.member.dto.MemberResponse;
import com.example.cleanmate.domain.member.dto.RenameMemberRequest;
import com.example.cleanmate.global.dto.ReorderRequest;
import com.example.cleanmate.domain.member.dto.ResetPasswordRequest;
import com.example.cleanmate.domain.member.service.MemberService;
import com.example.cleanmate.global.apiPayload.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping
    public ApiResponse<List<MemberResponse>> getMembers(
            @CookieValue(name = "CMSESSION", required = false) String sessionId) {
        return ApiResponse.ok(memberService.getMembers(sessionId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> addMember(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @Valid @RequestBody AddMemberRequest request) {
        memberService.addMember(sessionId, request.name(), request.password());
        return ApiResponse.ok();
    }

    @PutMapping("/order")
    public ApiResponse<Void> reorderMembers(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @Valid @RequestBody ReorderRequest request) {
        memberService.reorderMembers(sessionId, request.ids());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> renameMember(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @PathVariable Long id,
            @Valid @RequestBody RenameMemberRequest request) {
        memberService.renameMember(sessionId, id, request.name());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMember(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @PathVariable Long id) {
        memberService.deleteMember(sessionId, id);
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(
            @CookieValue(name = "CMSESSION", required = false) String sessionId,
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        memberService.resetPassword(sessionId, id, request.password());
        return ApiResponse.ok();
    }
}
