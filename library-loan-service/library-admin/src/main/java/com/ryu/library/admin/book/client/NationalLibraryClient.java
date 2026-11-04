package com.ryu.library.admin.book.client;

import com.ryu.library.global.exception.BusinessException;
import com.ryu.library.global.exception.ErrorCode;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// 국립중앙도서관 서지 정보 제공 서비스(공공데이터포털, getbookList_v2) 클라이언트.
// - 연결 2초, 응답 5초 타임아웃. 외부 서비스가 느려도 관리자 요청 스레드가 무한정 묶이지 않게 합니다.
// - 요청 URL에 인증키가 들어가므로 예외 메시지(URL 포함)를 로그에 그대로 남기지 않습니다.
// - 같은 필드가 문자열일 때도 배열일 때도 있어 고정된 타입 대신 JSON 트리로 읽습니다.
@Slf4j
@Component
@EnableConfigurationProperties(NationalLibraryProperties.class)
public class NationalLibraryClient {

    private static final String SUCCESS = "00";
    private static final String NO_DATA = "03";
    // 공공데이터포털 게이트웨이 오류 중 인증키 문제: 30 등록되지 않은 키, 31 기한 만료
    private static final Set<String> INVALID_KEY_REASONS = Set.of("30", "31");

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final NationalLibraryProperties properties;

    public NationalLibraryClient(RestClient.Builder builder, JsonMapper jsonMapper, NationalLibraryProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        this.restClient = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
        this.jsonMapper = jsonMapper;
        this.properties = properties;
    }

    public Optional<NlBookInfo> findByIsbn(String isbn) {
        if (!properties.isConfigured()) {
            throw new BusinessException(ErrorCode.NL_API_NOT_CONFIGURED);
        }
        RawResponse response = call(isbn);
        JsonNode root = parse(response.body());

        // 인증키 오류 같은 게이트웨이 오류는 4xx와 함께 OpenAPI_ServiceResponse 형식으로 옵니다.
        JsonNode gatewayError = root.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");
        if (!gatewayError.isMissingNode()) {
            String reason = gatewayError.path("returnReasonCode").asString("");
            log.warn("nl api gateway error. status={}, reason={}, errMsg={}",
                    response.status(), reason, gatewayError.path("errMsg").asString(""));
            throw new BusinessException(INVALID_KEY_REASONS.contains(reason)
                    ? ErrorCode.NL_API_INVALID_KEY : ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }
        if (response.status() / 100 != 2) {
            log.warn("nl api http error. status={}", response.status());
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }

        // 결과가 없어도 HTTP 200이고 resultCode=03으로 알려 줍니다.
        String resultCode = root.path("header").path("resultCode").asString("");
        if (NO_DATA.equals(resultCode)) {
            return Optional.empty();
        }
        if (!SUCCESS.equals(resultCode)) {
            log.warn("nl api result error. resultCode={}, resultMsg={}",
                    resultCode, root.path("header").path("resultMsg").asString(""));
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }
        return elements(root.path("body").path("items")).stream()
                .filter(item -> values(item.path("BIBO_isbn")).contains(isbn))
                .findFirst()
                .map(item -> toBookInfo(isbn, item));
    }

    private RawResponse call(String isbn) {
        try {
            return restClient.get()
                    .uri(uri -> uri.path("/getbookList_v2")
                            .queryParam("serviceKey", properties.certKey())
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 10)
                            .queryParam("type", "json")
                            .queryParam("isbn", isbn)
                            .build())
                    .exchange((request, response) -> new RawResponse(response.getStatusCode().value(),
                            new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8)));
        } catch (RestClientException e) {
            log.warn("nl api call failed. isbn={}, cause={}", isbn, e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }
    }

    private JsonNode parse(String body) {
        try {
            return jsonMapper.readTree(body);
        } catch (JacksonException e) {
            log.warn("nl api response is not json. cause={}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.EXTERNAL_API_UNAVAILABLE);
        }
    }

    private static NlBookInfo toBookInfo(String isbn, JsonNode item) {
        return new NlBookInfo(
                isbn,
                first(item.path("DCTERMS_title")),
                author(item),
                first(item.path("DC_publisher")),
                year(first(item.path("NLON_issuedYear"))),
                joined(item.path("DCTERMS_abstract")),
                first(item.path("NLON_kdc")));
    }

    // RDFS_label은 "제목 / 조슈아 블로크 지음 ; 개앞맵시 옮김" 형식이라 '/' 뒤가 가장 정확한 저자 표시입니다.
    // 없으면 창작자 이름 목록(DC_creator)을 씁니다.
    private static String author(JsonNode item) {
        String label = first(item.path("RDFS_label"));
        if (label != null && label.contains(" / ")) {
            return label.substring(label.indexOf(" / ") + 3).trim();
        }
        List<String> creators = values(item.path("DC_creator"));
        return creators.isEmpty() ? null : String.join(", ", creators);
    }

    private static Integer year(String value) {
        if (value == null || !value.matches("\\d{4}")) {
            return null;
        }
        return Integer.valueOf(value);
    }

    private static List<JsonNode> elements(JsonNode node) {
        if (node.isArray()) {
            return StreamSupport.stream(node.spliterator(), false).toList();
        }
        return node.isObject() ? List.of(node) : List.of();
    }

    // 문자열 하나, 문자열 배열, null 어느 형태로 와도 문자열 목록으로 바꿉니다.
    private static List<String> values(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(element -> addIfText(result, element));
        } else {
            addIfText(result, node);
        }
        return result;
    }

    private static void addIfText(List<String> result, JsonNode node) {
        if (node.isString() && !node.asString().isBlank()) {
            result.add(node.asString().trim());
        }
    }

    private static String first(JsonNode node) {
        List<String> values = values(node);
        return values.isEmpty() ? null : values.get(0);
    }

    private static String joined(JsonNode node) {
        List<String> values = values(node);
        return values.isEmpty() ? null : String.join("\n", values);
    }

    private record RawResponse(int status, String body) {
    }
}
