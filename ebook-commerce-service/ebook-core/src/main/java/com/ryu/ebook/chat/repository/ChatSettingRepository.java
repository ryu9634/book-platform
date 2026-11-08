package com.ryu.ebook.chat.repository;

import com.ryu.ebook.chat.domain.ChatSetting;
import org.springframework.data.jpa.repository.JpaRepository;

// 행은 하나뿐입니다. ChatSetting.SINGLE_ROW_ID로 찾습니다.
public interface ChatSettingRepository extends JpaRepository<ChatSetting, Long> {
}
