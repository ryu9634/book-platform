package com.ryu.ebook.global.policy;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 판매·환불 정책 값. 코드에 숫자를 두지 않고 application-core.yml의 ebook.policy에서 읽습니다.
@ConfigurationProperties(prefix = "ebook.policy")
public record EbookPolicy(int refundDays) {
}
