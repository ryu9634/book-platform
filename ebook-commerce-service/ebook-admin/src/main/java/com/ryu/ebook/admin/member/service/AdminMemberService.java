package com.ryu.ebook.admin.member.service;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import com.ryu.ebook.member.domain.Member;
import com.ryu.ebook.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;

    // 엔티티 대신 화면에 필요한 값만 담아 넘깁니다.
    public Page<MemberRow> search(String keyword, Pageable pageable) {
        return memberRepository.search(keyword == null || keyword.isBlank() ? null : keyword.trim(), pageable)
                .map(MemberRow::from);
    }

    public record MemberRow(Long id, String loginId, String name, String email, boolean admin, boolean active,
            java.time.LocalDateTime createdAt) {

        static MemberRow from(Member m) {
            return new MemberRow(m.getId(), m.getLoginId(), m.getName(), m.getEmail(), m.isAdmin(), !m.isWithdrawn(),
                    m.getCreatedAt());
        }
    }

    // 탈퇴 처리: 행은 남기고 상태만 바꿉니다(주문, 서재, 리뷰가 참조). 탈퇴 회원은 로그인할 수 없습니다.
    // 관리자가 자기 자신을 탈퇴시키면 관리자 화면에 다시 들어올 수 없으므로 막습니다.
    @Transactional
    public void withdraw(Long adminId, Long memberId) {
        if (adminId.equals(memberId)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.withdraw();
        log.info("member withdrawn by admin. memberId={}, by={}", memberId, adminId);
    }
}
