package com.ryu.library.admin.book.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

// 국립중앙도서관 ISBN 서지정보 API(SearchApi.do) 응답. 오류도 HTTP 200으로 오고 RESULT=ERROR로 구분합니다.
record NlSearchResponse(
        @JsonProperty("RESULT") String result,
        @JsonProperty("ERR_CODE") String errorCode,
        @JsonProperty("ERR_MESSAGE") String errorMessage,
        @JsonProperty("TOTAL_COUNT") Integer totalCount,
        @JsonProperty("docs") List<Doc> docs) {

    boolean isError() {
        return "ERROR".equalsIgnoreCase(result);
    }

    // 쓰는 필드만 받습니다. 나머지 필드는 무시합니다.
    record Doc(
            @JsonProperty("TITLE") String title,
            @JsonProperty("AUTHOR") String author,
            @JsonProperty("PUBLISHER") String publisher,
            @JsonProperty("EA_ISBN") String isbn,
            @JsonProperty("PUBLISH_PREDATE") String publishPredate,
            @JsonProperty("TITLE_URL") String titleUrl,
            @JsonProperty("BOOK_INTRODUCTION") String bookIntroduction,
            @JsonProperty("KDC") String kdc) {
    }
}
