package com.ryu.ebook.member.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ryu.ebook.global.config.ClockConfig;
import com.ryu.ebook.global.config.JpaConfig;
import com.ryu.ebook.member.MemberFixture;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, JpaConfig.class, ClockConfig.class})
class MemberRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Test
    @DisplayName("회원을 저장하면 시퀀스로 ID가 생기고 생성, 수정 시각이 기록된다")
    void save_새회원_ID와시각기록() {
        // given
        Member member = MemberFixture.activeMember();

        // when
        Member saved = memberRepository.saveAndFlush(member);

        // then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("로그인 아이디와 이메일로 가입 여부를 확인하고 회원을 찾는다")
    void existsAndFind_저장된회원_조회됨() {
        // given
        memberRepository.saveAndFlush(MemberFixture.activeMember("reader02", "reader02@example.com"));

        // when, then
        assertThat(memberRepository.existsByLoginId("reader02")).isTrue();
        assertThat(memberRepository.existsByEmail("reader02@example.com")).isTrue();
        assertThat(memberRepository.existsByLoginId("nobody")).isFalse();
        assertThat(memberRepository.findByLoginId("reader02")).isPresent();
    }
}
