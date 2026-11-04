package com.ryu.ebook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// 관리자용 화면과 API. 메인 클래스를 com.ryu.ebook 패키지에 두어 core 모듈의 엔티티, Repository, 설정까지 스캔합니다.
@SpringBootApplication
public class EbookAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(EbookAdminApplication.class, args);
    }
}
