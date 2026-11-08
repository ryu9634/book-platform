package com.ryu.ebook.chat.repository;

import com.ryu.ebook.chat.domain.ChatMessageEbook;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageEbookRepository extends JpaRepository<ChatMessageEbook, Long> {

    // 대화 이력을 그릴 때 메시지마다 조회하지 않고 한 번에 가져옵니다(N+1 방지).
    List<ChatMessageEbook> findByChatMessageIdInOrderBySortOrderAsc(Collection<Long> chatMessageIds);
}
