package com.ryu.ebook.user.chat.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatProperties.class)
public class ChatConfig {

    // 스트리밍 응답은 모델이 답을 다 쓸 때까지 요청 스레드를 붙잡습니다.
    // 가상 스레드로 돌리면 기다리는 동안 플랫폼 스레드를 점유하지 않습니다(Java 21).
    // 종료할 때는 남은 작업을 기다리지 않습니다. 기다려도 회원은 이미 화면을 떠난 상태입니다.
    @Bean(destroyMethod = "shutdownNow")
    public ExecutorService chatStreamExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
