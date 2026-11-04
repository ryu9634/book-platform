package com.ryu.library.admin.global.init;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 첫 관리자 계정 정보. 값은 환경 변수(.env)나 git에 올리지 않는 application-local.yml에서만 넣습니다.
@ConfigurationProperties(prefix = "library.admin")
public record AdminAccountProperties(String initialLoginId, String initialPassword) {

    public boolean isConfigured() {
        return initialLoginId != null && !initialLoginId.isBlank()
                && initialPassword != null && !initialPassword.isBlank();
    }
}
