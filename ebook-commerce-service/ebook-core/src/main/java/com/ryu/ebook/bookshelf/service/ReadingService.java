package com.ryu.ebook.bookshelf.service;

import com.ryu.ebook.bookshelf.domain.Bookshelf;
import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.ebook.storage.EbookFileStorage;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 뷰어: 내 서재에 있는(활성) 전자책만 읽을 수 있습니다. 판매가 중지되거나 숨겨진 전자책도 이미 산 회원은 읽습니다.
@Service
@RequiredArgsConstructor
public class ReadingService {

    private final BookshelfRepository bookshelfRepository;
    private final EbookRepository ebookRepository;
    private final EbookFileStorage fileStorage;
    private final Clock clock;

    // 페이지를 읽으면 그 위치를 기록합니다(이어 읽기). 읽은 기록이 생기면 그 항목은 환불할 수 없습니다.
    @Transactional
    public ReadingPage read(Long memberId, Long bookshelfId, int page) {
        Bookshelf shelf = bookshelfRepository.findById(bookshelfId)
                .filter(s -> s.isOwnedBy(memberId) && s.isActive())
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKSHELF_NOT_FOUND));
        Ebook ebook = ebookRepository.findById(shelf.getEbookId())
                .orElseThrow(() -> new BusinessException(ErrorCode.EBOOK_NOT_FOUND));
        if (!ebook.hasFile()) {
            throw new BusinessException(ErrorCode.CONTENT_NOT_READY);
        }
        int requested = page > 0 ? page : Math.max(1, shelf.getLastReadPage());
        EbookFileStorage.Page content = fileStorage.readPage(ebook.getFileKey(), requested);
        shelf.recordReading(content.page(), LocalDateTime.now(clock));
        return new ReadingPage(bookshelfId, ebook.getTitle(), content.page(), content.totalPages(), content.content());
    }

    public record ReadingPage(Long bookshelfId, String title, int page, int totalPages, String content) {

        public boolean hasPrevious() {
            return page > 1;
        }

        public boolean hasNext() {
            return page < totalPages;
        }
    }
}
