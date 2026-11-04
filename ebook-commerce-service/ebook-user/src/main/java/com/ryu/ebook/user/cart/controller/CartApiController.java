package com.ryu.ebook.user.cart.controller;

import com.ryu.ebook.global.common.ApiResponse;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.user.cart.dto.CartResponse;
import com.ryu.ebook.user.cart.service.CartService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/cart")
public class CartApiController {

    private final CartService cartService;

    @GetMapping
    public ApiResponse<CartResponse> cart(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(cartService.get(Long.valueOf(jwt.getSubject())));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<Void>> add(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, Long> request) {
        Long ebookId = request.get("ebookId");
        if (ebookId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        cartService.add(Long.valueOf(jwt.getSubject()), ebookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok());
    }

    @DeleteMapping("/items/{cartItemId}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long cartItemId) {
        cartService.remove(Long.valueOf(jwt.getSubject()), cartItemId);
        return ApiResponse.ok();
    }
}
