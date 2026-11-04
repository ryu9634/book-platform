package com.ryu.ebook;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.ebook.support.TestcontainersConfiguration;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class FlywayMigrationTest {

    @Autowired
    Flyway flyway;

    @Test
    @DisplayName("모든 마이그레이션이 적용되고 Hibernate 스키마 검증을 통과한다")
    void migrate_빈스키마_모두적용() {
        // given, when: 컨텍스트 시작 시 Flyway가 마이그레이션을 적용하고 Hibernate가 스키마를 검증

        // then
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(flyway.info().applied()).isNotEmpty();
    }
}
