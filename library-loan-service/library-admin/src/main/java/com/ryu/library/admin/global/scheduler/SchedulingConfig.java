package com.ryu.library.admin.global.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 정기 작업은 관리자 앱에서만 실행합니다. 회원 앱까지 돌리면 같은 작업이 두 번 실행됩니다.
// 관리자 앱을 여러 대 띄우게 되면 ShedLock 같은 분산 락이 필요합니다.
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "library.schedule", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {
}
