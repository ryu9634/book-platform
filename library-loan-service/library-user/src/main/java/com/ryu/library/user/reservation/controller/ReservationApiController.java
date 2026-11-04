package com.ryu.library.user.reservation.controller;

import com.ryu.library.global.common.ApiResponse;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.reservation.service.ReservationService;
import com.ryu.library.user.reservation.dto.ReservationResponse;
import com.ryu.library.user.reservation.service.ReservationQueryService;
import java.util.List;
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
@RequestMapping("/api/v1/reservations")
public class ReservationApiController {

    private final ReservationService reservationService;
    private final ReservationQueryService reservationQueryService;

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Long>>> reserve(@AuthenticationPrincipal Jwt jwt,
            @RequestBody Map<String, Long> request) {
        Long bookId = request.get("bookId");
        if (bookId == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Long reservationId = reservationService.reserve(Long.valueOf(jwt.getSubject()), bookId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of("reservationId", reservationId)));
    }

    @DeleteMapping("/{reservationId}")
    public ApiResponse<Void> cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long reservationId) {
        reservationService.cancel(Long.valueOf(jwt.getSubject()), reservationId);
        return ApiResponse.ok();
    }

    @GetMapping("/my")
    public ApiResponse<List<ReservationResponse>> myReservations(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(reservationQueryService.myActiveReservations(Long.valueOf(jwt.getSubject())));
    }
}
