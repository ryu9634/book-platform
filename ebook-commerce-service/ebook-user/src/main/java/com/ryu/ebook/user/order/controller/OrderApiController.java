package com.ryu.ebook.user.order.controller;

import com.ryu.ebook.global.common.ApiResponse;
import com.ryu.ebook.global.common.PageResponse;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.order.service.OrderService;
import com.ryu.ebook.payment.service.PaymentService;
import com.ryu.ebook.user.order.dto.OrderDetailResponse;
import com.ryu.ebook.user.order.dto.OrderRequest;
import com.ryu.ebook.user.order.dto.OrderSummaryResponse;
import com.ryu.ebook.user.order.service.OrderQueryService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> create(@AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody OrderRequest request) {
        String orderNo = orderService.create(memberId(jwt), request.ebookIds(), request.method());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("orderNo", orderNo)));
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderSummaryResponse>> myOrders(@AuthenticationPrincipal Jwt jwt,
            @PageableDefault(sort = "orderedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(PageResponse.from(orderQueryService.myOrders(memberId(jwt), pageable)));
    }

    @GetMapping("/{orderNo}")
    public ApiResponse<OrderDetailResponse> myOrder(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNo) {
        return ApiResponse.ok(orderQueryService.myOrder(memberId(jwt), orderNo));
    }

    // 실패는 402로 알리지만, 결제 실패 기록(FAILED)은 이미 커밋된 뒤라 다시 승인할 수 있습니다.
    @PostMapping("/{orderNo}/payment/approve")
    public ApiResponse<Void> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNo,
            @RequestBody(required = false) Map<String, Boolean> request) {
        boolean simulateFailure = request != null && Boolean.TRUE.equals(request.get("simulateFailure"));
        PaymentService.PaymentOutcome outcome = paymentService.approve(memberId(jwt), orderNo, simulateFailure);
        if (!outcome.approved()) {
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, outcome.failReason());
        }
        return ApiResponse.ok();
    }

    @PostMapping("/{orderNo}/cancel")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String orderNo) {
        orderService.cancel(memberId(jwt), orderNo);
        return ApiResponse.ok();
    }

    private static Long memberId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
