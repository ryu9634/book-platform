package com.ryu.library.user.member.service;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.repository.MemberRepository;
import com.ryu.library.user.member.dto.MemberResponse;
import com.ryu.library.user.member.dto.MemberSignupRequest;
import com.ryu.library.user.member.dto.MyPageResponse;
import java.time.Clock;
import java.time.LocalDate;
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
    private final Clock clock;

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

    // 현재 비밀번호가 맞을 때만 바꿉니다. 새 비밀번호도 BCrypt로 암호화합니다.
    @Transactional
    public void changePassword(Long memberId, String currentPassword, String newPassword) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        if (!passwordEncoder.matches(currentPassword, member.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
        member.changePassword(passwordEncoder.encode(newPassword));
        log.info("member password changed. memberId={}", memberId);
    }

    public MyPageResponse getMyPage(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        boolean suspended = member.isSuspendedOn(LocalDate.now(clock));
        return new MyPageResponse(member.getLoginId(), member.getName(), member.getEmail(), member.getPhone(),
                suspended, suspended ? member.getSuspendedUntil() : null);
    }

    public MemberResponse getMyInfo(Long memberId) {
        return memberRepository.findById(memberId)
                .map(MemberResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
