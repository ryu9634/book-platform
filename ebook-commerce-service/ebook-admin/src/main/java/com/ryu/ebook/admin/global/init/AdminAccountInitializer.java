package com.ryu.ebook.admin.global.init;

import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

// 관리자 계정은 마이그레이션에 넣지 않고(비밀번호 해시가 저장소에 남지 않도록) 앱 시작 시 한 번 만듭니다.
// 설정이 비어 있거나 같은 아이디가 이미 있으면 아무것도 하지 않습니다.
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(AdminAccountProperties.class)
public class AdminAccountInitializer implements ApplicationRunner {

    private final AdminAccountProperties properties;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isConfigured()) {
            log.info("initial admin account is not configured. skip.");
            return;
        }
        if (memberRepository.existsByLoginId(properties.initialLoginId())) {
            return;
        }
        Member admin = Member.createAdmin(
                properties.initialLoginId(),
                passwordEncoder.encode(properties.initialPassword()),
                "관리자",
                properties.initialLoginId() + "@admin.ebook.local");
        memberRepository.save(admin);
        log.info("initial admin account created. memberId={}", admin.getId());
    }
}
