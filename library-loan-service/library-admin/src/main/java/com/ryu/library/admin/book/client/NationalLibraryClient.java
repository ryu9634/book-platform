package com.ryu.library.admin.book.client;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

// 국립중앙도서관 ISBN 서지정보 API 클라이언트.
// - 연결 2초, 응답 5초 타임아웃. 외부 서비스가 느려도 관리자 요청 스레드가 무한정 묶이지 않게 합니다.
// - 요청 URL에 인증키가 들어가므로 예외 메시지(URL 포함)를 로그에 그대로 남기지 않습니다.
@Slf4j
@Component
@EnableConfigurationProperties(NationalLibraryProperties.class)
public class NationalLibraryClient {

    private static final DateTimeFormatter PUBLISH_DATE = DateTimeFormatter.BASIC_ISO_DATE;

    private final RestClient restClient;
    private final NationalLibraryProperties properties;

    public NationalLibraryClient(RestClient.Builder builder, NationalLibraryProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
        this.properties = properties;
    }

    public Optional<NlBookInfo> findByIsbn(String isbn) {
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.NL_API_NOT_CONFIGURED);
        }
        NlSearchResponse response = call(isbn);
        if (response == null || response.isError()) {
            log.warn("nl api error. errCode={}, errMessage={}",
                    response == null ? null : response.errorCode(), response == null ? null : response.errorMessage());
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }
        List<NlSearchResponse.Doc> docs = response.docs() == null ? List.of() : response.docs();
        return docs.stream()
                .filter(doc -> isbn.equals(doc.isbn()))
                .findFirst()
                .map(NationalLibraryClient::toBookInfo);
    }

    private NlSearchResponse call(String isbn) {
        try {
            return restClient.get()
                    .uri(uri -> uri.path("/seoji/SearchApi.do")
                            .queryParam("cert_key", properties.certKey())
                            .queryParam("result_style", "json")
                            .queryParam("page_no", 1)
                            .queryParam("page_size", 10)
                            .queryParam("isbn", isbn)
                            .build())
                    .retrieve()
                    .body(NlSearchResponse.class);
        } catch (RestClientException e) {
            log.warn("nl api call failed. isbn={}, cause={}", isbn, e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }
    }

    private static NlBookInfo toBookInfo(NlSearchResponse.Doc doc) {
        return new NlBookInfo(
                doc.isbn(),
                blankToNull(doc.title()),
                blankToNull(doc.author()),
                blankToNull(doc.publisher()),
                parseDate(doc.publishPredate()),
                blankToNull(doc.titleUrl()),
                blankToNull(doc.bookIntroduction()),
                blankToNull(doc.kdc()));
    }

    // 출판예정일은 yyyyMMdd 형식이지만 비어 있거나 형식이 다른 경우가 있어 그때는 비워 둡니다.
    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), PUBLISH_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
