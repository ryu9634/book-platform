package com.ryu.ebook.user.member.controller;

import com.ryu.ebook.global.common.ApiResponse;
import com.ryu.ebook.user.member.dto.MemberResponse;
import com.ryu.ebook.user.member.dto.MemberSignupRequest;
import com.ryu.ebook.user.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/members")
public class MemberApiController {

    private final MemberService memberService;

    @PostMapping
    public ResponseEntity<ApiResponse<MemberResponse>> signup(@Valid @RequestBody MemberSignupRequest request) {
        MemberResponse response = memberService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    // Access Token의 sub(회원 ID)로 본인 정보만 조회합니다. URL로 회원 ID를 받지 않습니다.
    @GetMapping("/me")
    public ApiResponse<MemberResponse> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(memberService.getMyInfo(Long.valueOf(jwt.getSubject())));
    }
}
