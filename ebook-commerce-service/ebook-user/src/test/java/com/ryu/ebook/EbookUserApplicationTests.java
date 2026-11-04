package com.ryu.ebook;

import com.ryu.ebook.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@SpringBootTest
class EbookUserApplicationTests {

    @Test
    @DisplayName("회원용 화면과 API 애플리케이션 컨텍스트가 뜬다")
    void contextLoads() {
    }
}
