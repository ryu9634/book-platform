package com.ryu.ebook.chat.repository;

import com.ryu.ebook.chat.domain.ChatPrompt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatPromptRepository extends JpaRepository<ChatPrompt, Long> {

    Optional<ChatPrompt> findByPromptKey(String promptKey);

    List<ChatPrompt> findAllByOrderByIdAsc();
}
