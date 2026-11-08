package com.ryu.ebook.chat.repository;

import com.ryu.ebook.chat.domain.ChatSession;
import com.ryu.ebook.chat.domain.ChatSessionStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    // 진행 중인 세션. 과거에 닫지 못한 세션이 여럿 남아 있어도 가장 최근 것을 씁니다.
    Optional<ChatSession> findFirstByMemberIdAndStatusOrderByIdDesc(Long memberId, ChatSessionStatus status);

    Page<ChatSession> findByMemberIdOrderByIdDesc(Long memberId, Pageable pageable);
}
