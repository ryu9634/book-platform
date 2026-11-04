package com.ryu.library.global.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    // 시간이 관여하는 로직은 LocalDateTime.now(clock)으로 써서 테스트에서 시각을 고정할 수 있게 합니다.
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
