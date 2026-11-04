package com.ryu.library.user.member.service;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.user.member.dto.MemberResponse;
import com.ryu.library.user.member.dto.MemberSignupRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    // 동시에 같은 아이디로 가입하면 둘 다 중복 확인을 통과할 수 있습니다.
    // 그 경우 UNIQUE 제약이 막고 GlobalExceptionHandler가 409로 바꿉니다.
    @Transactional
    public MemberResponse signup(MemberSignupRequest request) {
        if (memberRepository.existsByLoginId(request.loginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (memberRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        Member member = Member.create(
                request.loginId(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.email(),
                request.phone());
        Member saved = memberRepository.save(member);

        log.info("member signed up. memberId={}", saved.getId());
        return MemberResponse.from(saved);
    }
}
