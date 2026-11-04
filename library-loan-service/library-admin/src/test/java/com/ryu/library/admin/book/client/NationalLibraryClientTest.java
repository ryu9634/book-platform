package com.ryu.library.admin.book.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

class NationalLibraryClientTest {

    @RegisterExtension
    static WireMockExtension nl = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final String ISBN = "9788966262281";

    private NationalLibraryClient client(String certKey) {
        NationalLibraryProperties properties = new NationalLibraryProperties(
                nl.baseUrl(), certKey, Duration.ofSeconds(1), Duration.ofMillis(500));
        return new NationalLibraryClient(RestClient.builder(), properties);
    }

    @Test
    @DisplayName("ISBN으로 조회하면 서지 정보를 우리 형식으로 바꿔 돌려주고, 인증키를 요청에 담는다")
    void findByIsbn_정상응답_변환() {
        // given
        nl.stubFor(get(urlPathEqualTo("/seoji/SearchApi.do")).willReturn(okJson("""
                {"PAGE_NO":"1","TOTAL_COUNT":"1","docs":[{
                  "TITLE":"클린 코드","AUTHOR":"로버트 C. 마틴 지음 ; 박재호 옮김","PUBLISHER":"인사이트",
                  "EA_ISBN":"9788966262281","PUBLISH_PREDATE":"20131224","TITLE_URL":"https://example.com/cover.jpg",
                  "BOOK_INTRODUCTION":"","KDC":"005.1","SOMETHING_NEW":"무시"}]}
                """)));

        // when
        Optional<NlBookInfo> result = client("test-key").findByIsbn(ISBN);

        // then
        assertThat(result).hasValueSatisfying(info -> {
            assertThat(info.title()).isEqualTo("클린 코드");
            assertThat(info.publisher()).isEqualTo("인사이트");
            assertThat(info.publishedDate()).isEqualTo(LocalDate.of(2013, 12, 24));
            assertThat(info.coverImageUrl()).isEqualTo("https://example.com/cover.jpg");
            assertThat(info.description()).isNull();
        });
        nl.verify(getRequestedFor(urlPathEqualTo("/seoji/SearchApi.do"))
                .withQueryParam("cert_key", equalTo("test-key"))
                .withQueryParam("isbn", equalTo(ISBN))
                .withQueryParam("result_style", equalTo("json")));
    }

    @Test
    @DisplayName("검색 결과가 없거나 ISBN이 정확히 일치하는 도서가 없으면 빈 결과를 돌려준다")
    void findByIsbn_결과없음_빈값() {
        // given (ISBN은 앞부분 일치 검색이라 다른 ISBN이 섞여 올 수 있음)
        nl.stubFor(get(urlPathEqualTo("/seoji/SearchApi.do")).willReturn(okJson("""
                {"PAGE_NO":"1","TOTAL_COUNT":"1","docs":[{"TITLE":"다른 책","EA_ISBN":"9788966262299"}]}
                """)));

        // when, then
        assertThat(client("test-key").findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("HTTP 200이어도 본문이 RESULT=ERROR면 외부 서비스 오류로 처리한다")
    void findByIsbn_본문오류_C008() {
        // given
        nl.stubFor(get(urlPathEqualTo("/seoji/SearchApi.do")).willReturn(okJson("""
                {"RESULT":"ERROR","ERR_CODE":"011","ERR_MESSAGE":"유효하지 않은 인증키 값입니다."}
                """)));

        // when, then
        assertThatThrownBy(() -> client("wrong-key").findByIsbn(ISBN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("외부 서버가 500을 돌려주면 외부 서비스 오류로 처리한다")
    void findByIsbn_500_C008() {
        // given
        nl.stubFor(get(urlPathEqualTo("/seoji/SearchApi.do")).willReturn(aResponse().withStatus(500)));

        // when, then
        assertThatThrownBy(() -> client("test-key").findByIsbn(ISBN))
                .extracting("errorCode").isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("응답이 응답 타임아웃보다 늦으면 기다리지 않고 외부 서비스 오류로 처리한다")
    void findByIsbn_느린응답_타임아웃() {
        // given (응답 타임아웃 0.5초, 서버 지연 3초)
        nl.stubFor(get(urlPathEqualTo("/seoji/SearchApi.do"))
                .willReturn(okJson("{\"TOTAL_COUNT\":0,\"docs\":[]}").withFixedDelay(3000)));

        // when
        long start = System.nanoTime();
        assertThatThrownBy(() -> client("test-key").findByIsbn(ISBN))
                .extracting("errorCode").isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE);

        // then
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("인증키가 설정되지 않았으면 외부 서버를 호출하지 않고 B004로 알린다")
    void findByIsbn_인증키없음_B004() {
        // when, then
        assertThatThrownBy(() -> client("").findByIsbn(ISBN))
                .extracting("errorCode").isEqualTo(ErrorCode.NL_API_NOT_CONFIGURED);
        assertThat(nl.getAllServeEvents()).isEmpty();
    }
}
