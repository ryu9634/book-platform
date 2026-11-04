package com.ryu.library.user.reservation.controller;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.reservation.service.ReservationService;
import com.ryu.library.user.global.security.LoginMember;
import com.ryu.library.user.reservation.service.ReservationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ReservationViewController {

    private final ReservationService reservationService;
    private final ReservationQueryService reservationQueryService;

    @PostMapping("/reservations")
    public String reserve(@AuthenticationPrincipal LoginMember loginMember, @RequestParam Long bookId,
            RedirectAttributes redirectAttributes) {
        try {
            reservationService.reserve(loginMember.getMemberId(), bookId);
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/books/" + bookId;
        } catch (DataIntegrityViolationException e) {
            // 같은 예약을 동시에 두 번 보낸 경우 활성 예약 유니크 인덱스가 막습니다.
            redirectAttributes.addFlashAttribute("errorMessage", "이미 예약한 도서입니다.");
            return "redirect:/books/" + bookId;
        }
        redirectAttributes.addFlashAttribute("message", "예약했습니다. 반납되면 수령 가능 상태로 바뀝니다.");
        return "redirect:/reservations/my";
    }

    @GetMapping("/reservations/my")
    public String myReservations(@AuthenticationPrincipal LoginMember loginMember, Model model) {
        model.addAttribute("reservations", reservationQueryService.myActiveReservations(loginMember.getMemberId()));
        return "reservation/my";
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    public String cancel(@AuthenticationPrincipal LoginMember loginMember, @PathVariable Long reservationId,
            RedirectAttributes redirectAttributes) {
        try {
            reservationService.cancel(loginMember.getMemberId(), reservationId);
            redirectAttributes.addFlashAttribute("message", "예약을 취소했습니다.");
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/reservations/my";
    }
}
