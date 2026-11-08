package com.ryu.ebook.chat.repository;

import com.ryu.ebook.chat.domain.ChatMessage;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByChatSessionIdOrderBySeqNoAsc(Long chatSessionId);

    // 대화 기억으로 다시 보낼 최근 메시지. 최신순으로 가져와 서비스에서 뒤집습니다.
    List<ChatMessage> findByChatSessionIdOrderBySeqNoDesc(Long chatSessionId, Pageable pageable);

    void deleteByChatSessionId(Long chatSessionId);

    @Query("select coalesce(max(m.seqNo), 0) from ChatMessage m where m.chatSessionId = :chatSessionId")
    int findLastSeqNo(@Param("chatSessionId") Long chatSessionId);
}
