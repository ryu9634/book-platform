package com.ryu.library.admin.book.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

// 응답 본문은 실제 getbookList_v2 응답(2026-11)을 줄여서 씁니다.
class NationalLibraryClientTest {

    @RegisterExtension
    static WireMockExtension nl = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final String PATH = "/getbookList_v2";
    private static final String ISBN = "9788966262281";

    // 정상 케이스는 넉넉한 타임아웃을 씁니다. CI 러너의 첫 연결이 느려 짧은 타임아웃이면 정상 응답도 실패할 수 있습니다.
    private NationalLibraryClient client(String serviceKey) {
        return client(serviceKey, Duration.ofSeconds(5));
    }

    private NationalLibraryClient client(String serviceKey, Duration readTimeout) {
        NationalLibraryProperties properties = new NationalLibraryProperties(
                nl.baseUrl(), serviceKey, Duration.ofSeconds(5), readTimeout);
        return new NationalLibraryClient(RestClient.builder(), JsonMapper.builder().build(), properties);
    }

    @Test
    @DisplayName("ISBN으로 조회하면 서지 정보를 우리 형식으로 바꾸고, 문자열과 배열이 섞인 필드도 읽는다")
    void findByIsbn_정상응답_변환() {
        // given
        nl.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"header":{"resultCode":"00","resultMsg":"NORMAL_CODE"},
                 "body":{"totalCount":1,"pageNo":1,"numOfRows":10,"items":[{
                   "BIBLIO_ID":"KMO201855924","DCTERMS_title":"이펙티브 자바",
                   "RDFS_label":"이펙티브 자바 / 조슈아 블로크 지음 ; 개앞맵시 옮김",
                   "DC_creator":["이복연","Bloch, Joshua"],"DC_publisher":"인사이트",
                   "BIBO_isbn":"9788966262281","NLON_issuedYear":"2018","NLON_kdc":"005.133",
                   "DCTERMS_abstract":["첫 문단","둘째 문단"],"OWL_sameAs":null}]}}
                """)));

        // when
        Optional<NlBookInfo> result = client("test-key").findByIsbn(ISBN);

        // then
        assertThat(result).hasValueSatisfying(info -> {
            assertThat(info.title()).isEqualTo("이펙티브 자바");
            assertThat(info.author()).isEqualTo("조슈아 블로크 지음 ; 개앞맵시 옮김");
            assertThat(info.publisher()).isEqualTo("인사이트");
            assertThat(info.publishedYear()).isEqualTo(2018);
            assertThat(info.description()).isEqualTo("첫 문단\n둘째 문단");
            assertThat(info.kdc()).isEqualTo("005.133");
        });
        nl.verify(getRequestedFor(urlPathEqualTo(PATH))
                .withQueryParam("serviceKey", equalTo("test-key"))
                .withQueryParam("isbn", equalTo(ISBN))
                .withQueryParam("type", equalTo("json")));
    }

    @Test
    @DisplayName("RDFS_label에 저자 표시가 없으면 창작자 이름 목록을 저자로 쓴다")
    void findByIsbn_라벨에저자없음_창작자목록() {
        // given
        nl.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"header":{"resultCode":"00"},"body":{"items":[{
                  "DCTERMS_title":"제목","RDFS_label":"제목","DC_creator":"홍길동","BIBO_isbn":["9788966262281","9788966260000"]}]}}
                """)));

        // when, then
        assertThat(client("test-key").findByIsbn(ISBN)).hasValueSatisfying(info -> {
            assertThat(info.author()).isEqualTo("홍길동");
            assertThat(info.publishedYear()).isNull();
        });
    }

    @Test
    @DisplayName("결과가 없으면(resultCode=03, items=null) 빈 결과를 돌려준다")
    void findByIsbn_결과없음_빈값() {
        // given
        nl.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"header":{"resultCode":"03","resultMsg":"NODATA_ERROR"},"body":{"totalCount":0,"items":null}}
                """)));

        // when, then
        assertThat(client("test-key").findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("ISBN이 정확히 일치하는 도서가 없으면 빈 결과를 돌려준다")
    void findByIsbn_ISBN불일치_빈값() {
        // given
        nl.stubFor(get(urlPathEqualTo(PATH)).willReturn(okJson("""
                {"header":{"resultCode":"00"},"body":{"items":[{"DCTERMS_title":"다른 책","BIBO_isbn":"9788966260000"}]}}
                """)));

        // when, then
        assertThat(client("test-key").findByIsbn(ISBN)).isEmpty();
    }

    @Test
    @DisplayName("등록되지 않은 인증키(403, 사유 30)는 인증키 오류(B006)로 알린다")
    void findByIsbn_등록안된키_B006() {
        // given
        nl.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(403)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                        {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
                          "returnAuthMsg":"등록되지 않은 서비스키","returnReasonCode":"30"}}}
                        """)));

        // when, then
        assertThatThrownBy(() -> client("wrong-key").findByIsbn(ISBN))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NL_API_INVALID_KEY);
    }

    @Test
    @DisplayName("그 밖의 게이트웨이 오류, 5xx, JSON이 아닌 응답은 외부 서비스 오류(C008)로 처리한다")
    void findByIsbn_기타오류_C008() {
        // given: 서비스 경로 오류(사유 12)
        nl.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(400).withBody("""
                {"OpenAPI_ServiceResponse":{"cmmMsgHeader":{"errMsg":"NO_OPENAPI_SERVICE_ERROR","returnReasonCode":"12"}}}
                """)));
        assertThatThrownBy(() -> client("test-key").findByIsbn(ISBN))
                .extracting("errorCode").isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE);

        // given: 500 + HTML
        nl.stubFor(get(urlPathEqualTo(PATH)).willReturn(aResponse().withStatus(500).withBody("<html>error</html>")));
        assertThatThrownBy(() -> client("test-key").findByIsbn(ISBN))
                .extracting("errorCode").isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE);
    }

    @Test
    @DisplayName("응답이 응답 타임아웃보다 늦으면 기다리지 않고 외부 서비스 오류로 처리한다")
    void findByIsbn_느린응답_타임아웃() {
        // given (응답 타임아웃 0.5초, 서버 지연 3초)
        nl.stubFor(get(urlPathEqualTo(PATH))
                .willReturn(okJson("{\"header\":{\"resultCode\":\"03\"}}").withFixedDelay(3000)));
        NationalLibraryClient shortTimeout = client("test-key", Duration.ofMillis(500));

        // when
        long start = System.nanoTime();
        assertThatThrownBy(() -> shortTimeout.findByIsbn(ISBN))
                .extracting("errorCode").isEqualTo(ErrorCode.EXTERNAL_API_UNAVAILABLE);

        // then
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofMillis(2500));
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
