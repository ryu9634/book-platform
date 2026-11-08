package com.ryu.ebook.chat.repository;

import com.ryu.ebook.chat.domain.MemberChatSetting;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberChatSettingRepository extends JpaRepository<MemberChatSetting, Long> {

    Optional<MemberChatSetting> findByMemberId(Long memberId);

    // 관리자 회원 목록에서 한 번에 가져옵니다(N+1 방지).
    List<MemberChatSetting> findByMemberIdIn(Collection<Long> memberIds);
}
