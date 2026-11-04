package com.ryu.library.global.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 대출·예약 정책 값. 코드에 숫자를 박지 않고 application-core.yml의 library.policy에서 읽습니다.
@ConfigurationProperties(prefix = "library.policy")
public record LibraryPolicy(
        int maxLoanCount,
        int loanDays,
        int extendDays,
        int maxExtendCount,
        int reservationHoldDays) {
}
