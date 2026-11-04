package com.ryu.library.admin.loan.service;

import com.ryu.library.loan.domain.LoanStatus;
import com.ryu.library.loan.repository.AdminLoanView;
import com.ryu.library.loan.repository.LoanRepository;
import com.ryu.library.loan.service.LoanService;
import java.time.Clock;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminLoanService {

    private final LoanRepository loanRepository;
    private final LoanService loanService;
    private final Clock clock;

    // filter: ACTIVE(대출 중·연체), RETURNED, ALL
    public Page<AdminLoanView> search(String filter, String keyword, Pageable pageable) {
        Set<LoanStatus> statuses = switch (filter == null ? "ACTIVE" : filter) {
            case "RETURNED" -> EnumSet.of(LoanStatus.RETURNED);
            case "ALL" -> EnumSet.allOf(LoanStatus.class);
            default -> EnumSet.of(LoanStatus.ON_LOAN, LoanStatus.OVERDUE);
        };
        String normalized = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return loanRepository.searchForAdmin(statuses, normalized, pageable);
    }

    public List<AdminLoanView> overdue() {
        return loanRepository.findOverdueForAdmin(LocalDate.now(clock));
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    // 데스크 반납도 회원 반납과 같은 core 서비스를 써서 연체 정지와 예약 보관 규칙이 똑같이 적용됩니다.
    @Transactional
    public void returnByDesk(Long loanId) {
        loanService.returnByDesk(loanId);
    }
}
