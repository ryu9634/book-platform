package com.ryu.ebook.chat.domain;

// 대화 메시지를 쓴 쪽. 도구 호출과 추론 블록은 저장하지 않으므로 사람이 읽는 두 가지만 있습니다.
public enum ChatRole {
    USER,
    ASSISTANT
}
