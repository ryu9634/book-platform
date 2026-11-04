package com.ryu.library.admin.member.service;

import com.ryu.library.admin.member.dto.AdminMemberResponse;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.member.domain.Member;
import com.ryu.library.member.domain.MemberRole;
import com.ryu.library.member.repository.MemberRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;
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

    private static final int MAX_SUSPEND_DAYS = 365;

    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final Clock clock;

    // 회원 목록 1번 + 대출 중 권수 집계 1번 (회원마다 세지 않음)
    public Page<AdminMemberResponse> search(String keyword, Pageable pageable) {
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        Page<Member> members = memberRepository.search(normalized, pageable);
        Map<Long, Long> loanCounts = members.isEmpty() ? Map.of()
                : loanRepository.countActiveByMemberIds(members.map(Member::getId).getContent()).stream()
                        .collect(Collectors.toMap(LoanRepository.MemberLoanCount::getMemberId,
                                LoanRepository.MemberLoanCount::getCount));
        return members.map(member -> AdminMemberResponse.of(member, loanCounts.getOrDefault(member.getId(), 0L)));
    }

    @Transactional
    public void suspend(Long memberId, int days) {
        if (days < 1 || days > MAX_SUSPEND_DAYS) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Member member = find(memberId);
        member.suspendUntil(LocalDate.now(clock).plusDays(days));
        log.info("member suspended by admin. memberId={}, days={}", memberId, days);
    }

    @Transactional
    public void releaseSuspension(Long memberId) {
        find(memberId).releaseSuspension();
        log.info("member suspension released by admin. memberId={}", memberId);
    }

    // 관리자가 자기 권한을 내리면 관리자 화면에 다시 들어올 수 없으므로 막습니다.
    @Transactional
    public void changeRole(Long adminId, Long memberId, MemberRole role) {
        if (adminId.equals(memberId)) {
            throw new BusinessException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
        }
        find(memberId).changeRole(role);
        log.info("member role changed. memberId={}, role={}, by={}", memberId, role, adminId);
    }

    private Member find(Long memberId) {
        return memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
