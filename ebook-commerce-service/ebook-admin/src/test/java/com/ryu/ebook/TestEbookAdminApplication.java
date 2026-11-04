package com.ryu.ebook;

import com.ryu.ebook.support.TestcontainersConfiguration;
import org.springframework.boot.SpringApplication;

// 로컬 Oracle 없이 Testcontainers Oracle로 앱을 띄워 볼 때 실행합니다.
public class TestEbookAdminApplication {

    public static void main(String[] args) {
        SpringApplication.from(EbookAdminApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
