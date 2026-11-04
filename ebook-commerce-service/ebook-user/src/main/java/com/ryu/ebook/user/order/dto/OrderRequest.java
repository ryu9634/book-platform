package com.ryu.ebook.user.order.dto;

import com.ryu.ebook.payment.domain.PaymentMethod;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderRequest(@NotEmpty(message = "주문할 전자책을 골라 주세요.") List<Long> ebookIds, PaymentMethod method) {
}
