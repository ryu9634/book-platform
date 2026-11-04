package com.ryu.ebook.user.library.service;

import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.library.client.LibraryBookClient;
import com.ryu.ebook.user.library.client.LibraryClientProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

// "도서관에서 많이 빌린 책을 전자책으로": 도서관 서비스의 인기 도서를 ISBN으로 우리 전자책과 맞춥니다.
// - 결과는 일정 시간 캐시해 메인 화면을 열 때마다 다른 서비스를 부르지 않습니다.
// - 도서관 서비스가 느리거나 죽어도 메인 화면은 떠야 하므로, 실패하면 마지막 성공 결과(없으면 빈 목록)를 돌려줍니다(fallback).
@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryRecommendationService {

    private static final int LIMIT = 20;
    private static final int SHOW = 8;

    private final LibraryBookClient libraryBookClient;
    private final EbookRepository ebookRepository;
    private final LibraryClientProperties properties;
    private final Clock clock;

    private volatile Cached cached;

    public List<EbookSummaryResponse> recommend() {
        Cached current = cached;
        Instant now = clock.instant();
        if (current != null && now.isBefore(current.expiresAt())) {
            return current.items();
        }
        try {
            List<EbookSummaryResponse> items = match(fetchIsbns());
            cached = new Cached(items, now.plus(properties.cacheTtl()));
            return items;
        } catch (RestClientException e) {
            // 예외 메시지에는 내부 주소가 들어갈 수 있어 종류만 남깁니다.
            log.warn("library api unavailable. fallback to last result. cause={}", e.getClass().getSimpleName());
            return current == null ? List.of() : current.items();
        }
    }

    private List<String> fetchIsbns() {
        LibraryBookClient.PopularBooksResponse response = libraryBookClient.popular(LIMIT);
        if (response == null || response.data() == null) {
            return List.of();
        }
        return response.data().stream().map(LibraryBookClient.PopularBook::isbn).filter(Objects::nonNull).toList();
    }

    // 도서관 순위를 그대로 유지하면서 판매 중인 전자책만 남깁니다.
    private List<EbookSummaryResponse> match(List<String> isbns) {
        if (isbns.isEmpty()) {
            return List.of();
        }
        Map<String, Ebook> byIsbn = ebookRepository.findByIsbnIn(isbns).stream()
                .filter(Ebook::isOnSale)
                .collect(Collectors.toMap(Ebook::getIsbn, Function.identity(), (a, b) -> a));
        return isbns.stream().map(byIsbn::get).filter(Objects::nonNull).limit(SHOW)
                .map(ebook -> EbookSummaryResponse.of(ebook, null)).toList();
    }

    private record Cached(List<EbookSummaryResponse> items, Instant expiresAt) {
    }
}
