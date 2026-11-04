package com.ryu.ebook.bookshelf.repository;

import java.time.LocalDateTime;

// 내 서재 목록용 조회 결과. 서재는 전자책을 ID로만 참조하므로 조인 한 번으로 제목과 표지를 가져옵니다.
public record BookshelfView(Long bookshelfId, Long ebookId, String title, String author, String coverImageUrl,
        Integer pageCount, int lastReadPage, LocalDateTime lastReadAt, LocalDateTime acquiredAt) {
}
