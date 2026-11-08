package com.ryu.ebook.user.chat.client;

// 모델에 다시 실어 보낼 지난 대화 한 줄. 도구 호출 기록은 넘기지 않고 사람이 읽는 내용만 넘깁니다.
public record ChatTurn(boolean fromUser, String content) {
}
