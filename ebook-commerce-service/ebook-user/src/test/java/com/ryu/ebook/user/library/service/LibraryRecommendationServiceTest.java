package com.ryu.ebook.user.library.service;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ryu.ebook.ebook.EbookFixture;
import com.ryu.ebook.ebook.domain.Ebook;
import com.ryu.ebook.ebook.domain.EbookStatus;
import com.ryu.ebook.ebook.repository.EbookRepository;
import com.ryu.ebook.user.ebook.dto.EbookSummaryResponse;
import com.ryu.ebook.user.library.client.LibraryClientConfig;
import com.ryu.ebook.user.library.client.LibraryClientProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

// 도서관 서비스를 WireMock으로 대신해 정상, 오류, 지연, 캐시를 확인합니다.
class LibraryRecommendationServiceTest {

    @RegisterExtension
    static WireMockExtension library = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final String BODY = """
            {"success":true,"data":[
              {"bookId":1,"isbn":"9780000000002","title":"2위","author":"a","loanCount":5},
              {"bookId":2,"isbn":"9780000000001","title":"1위가 아니라 순서 확인","author":"b","loanCount":3},
              {"bookId":3,"isbn":"9780000000099","title":"전자책 없음","author":"c","loanCount":1}],"error":null}
            """;

    private final EbookRepository ebookRepository = mock(EbookRepository.class);
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-04T03:00:00Z"));
    private LibraryRecommendationService service;

    @BeforeEach
    void setUp() {
        LibraryClientProperties properties = new LibraryClientProperties(library.baseUrl(), Duration.ofSeconds(1),
                Duration.ofMillis(500), Duration.ofMinutes(5));
        var client = new LibraryClientConfig().libraryBookClient(RestClient.builder(), properties);
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneId.of("UTC"); }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now.get(); }
        };
        service = new LibraryRecommendationService(client, ebookRepository, properties, clock);
        Ebook first = EbookFixture.ebook("9780000000001", "전자책 1", 10000, 9000);
        Ebook second = EbookFixture.ebook("9780000000002", "전자책 2", 10000, 9000);
        Ebook stopped = EbookFixture.ebook("9780000000003", "판매 중지", 10000, 9000);
        stopped.changeStatus(EbookStatus.STOP_SALE);
        ReflectionTestUtils.setField(first, "id", 11L);
        ReflectionTestUtils.setField(second, "id", 12L);
        given(ebookRepository.findByIsbnIn(anyCollection())).willReturn(List.of(first, second, stopped));
    }

    @Test
    @DisplayName("도서관 인기 순위를 유지한 채 ISBN이 같은 판매 중인 전자책만 보여 주고, 캐시 기간에는 다시 부르지 않는다")
    void recommend_순위유지_캐시() {
        library.stubFor(get(urlPathEqualTo("/api/v1/books/popular")).willReturn(okJson(BODY)));

        List<EbookSummaryResponse> first = service.recommend();
        service.recommend();

        assertThat(first).extracting(EbookSummaryResponse::title).containsExactly("전자책 2", "전자책 1");
        library.verify(1, getRequestedFor(urlPathEqualTo("/api/v1/books/popular")));
    }

    @Test
    @DisplayName("도서관 서비스가 500을 돌려주거나 응답이 느리면 마지막 성공 결과로 대신한다 (fallback)")
    void recommend_장애_fallback() {
        library.stubFor(get(urlPathEqualTo("/api/v1/books/popular")).willReturn(okJson(BODY)));
        List<EbookSummaryResponse> ok = service.recommend();

        // 캐시가 끝난 뒤 도서관 서비스 장애
        now.set(now.get().plus(Duration.ofMinutes(6)));
        library.stubFor(get(urlPathEqualTo("/api/v1/books/popular")).willReturn(aResponse().withStatus(500)));
        assertThat(service.recommend()).isEqualTo(ok);

        library.stubFor(get(urlPathEqualTo("/api/v1/books/popular")).willReturn(okJson(BODY).withFixedDelay(3000)));
        long start = System.nanoTime();
        assertThat(service.recommend()).isEqualTo(ok);
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("처음부터 도서관 서비스에 연결할 수 없으면 빈 목록을 돌려주고 메인 화면은 그대로 뜬다")
    void recommend_첫호출실패_빈목록() {
        library.stubFor(get(urlPathEqualTo("/api/v1/books/popular")).willReturn(aResponse().withStatus(503)));

        assertThat(service.recommend()).isEmpty();
    }
}
