package com.ryu.ebook.user.bookshelf.service;

import com.ryu.ebook.bookshelf.repository.BookshelfRepository;
import com.ryu.ebook.bookshelf.repository.BookshelfView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookshelfQueryService {

    private final BookshelfRepository bookshelfRepository;

    public List<BookshelfView> myBookshelf(Long memberId) {
        return bookshelfRepository.findViewsByMember(memberId);
    }
}
