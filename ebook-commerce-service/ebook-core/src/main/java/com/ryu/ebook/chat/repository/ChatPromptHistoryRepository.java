package com.ryu.ebook.chat.repository;

import com.ryu.ebook.chat.domain.ChatPromptHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatPromptHistoryRepository extends JpaRepository<ChatPromptHistory, Long> {

    Page<ChatPromptHistory> findByChatPromptIdOrderByIdDesc(Long chatPromptId, Pageable pageable);
}
