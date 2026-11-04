package com.ryu.ebook.payment.gateway;

// 결제 대행사(PG) 연동 지점. 지금은 MockPaymentGateway를 쓰고, 실제 PG(토스페이먼츠 등)는 이 인터페이스를 구현해 바꿉니다.
public interface PaymentGateway {

    PgResult approve(String orderNo, long amount, boolean simulateFailure);

    PgResult cancel(String pgTid, long amount);

    record PgResult(boolean success, String tid, String failReason) {

        public static PgResult ok(String tid) {
            return new PgResult(true, tid, null);
        }

        public static PgResult fail(String reason) {
            return new PgResult(false, null, reason);
        }
    }
}
