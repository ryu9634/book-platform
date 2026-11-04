package com.ryu.library.user.loan.controller;

import com.ryu.library.global.common.ApiResponse;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.service.LoanService;
import com.ryu.library.user.loan.dto.LoanRequest;
import com.ryu.library.user.loan.dto.LoanResponse;
import com.ryu.library.user.loan.service.LoanQueryService;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/api/v1/loans")
public class LoanApiController {

    private final LoanService loanService;
    private final LoanQueryService loanQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Long>>> loan(@AuthenticationPrincipal Jwt jwt,
            @RequestBody LoanRequest request) {
        if (!request.isValid()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Long memberId = Long.valueOf(jwt.getSubject());
        Long loanId = request.bookId() != null
                ? loanService.loanBook(memberId, request.bookId())
                : loanService.loanCopy(memberId, request.bookCopyId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("loanId", loanId)));
    }

    @GetMapping("/my")
    public ApiResponse<List<LoanResponse>> myLoans(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(loanQueryService.myActiveLoans(Long.valueOf(jwt.getSubject())));
    }
}
