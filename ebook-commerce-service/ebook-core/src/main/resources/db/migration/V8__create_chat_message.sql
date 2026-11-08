-- 대화 메시지. 모델에 다시 보낼 대화 기억이면서 화면에 뿌리는 이력입니다.
-- 도구 호출 블록과 추론(thinking) 블록은 저장하지 않고 사람이 읽는 질문·답변만 남깁니다.
-- 토큰 수를 메시지에 남겨 두면 어떤 대화가 비쌌는지 나중에 되짚을 수 있습니다.
CREATE SEQUENCE CHAT_MESSAGE_SEQ START WITH 1 INCREMENT BY 50 NOCACHE;

CREATE TABLE CHAT_MESSAGE (
    CHAT_MESSAGE_ID  NUMBER(19)    NOT NULL,
    CHAT_SESSION_ID  NUMBER(19)    NOT NULL,   -- 논리 참조: CHAT_SESSION
    SEQ_NO           NUMBER(10)    NOT NULL,   -- 세션 안에서의 순서(1부터)
    ROLE             VARCHAR2(20)  NOT NULL,
    CONTENT          CLOB          NOT NULL,
    INPUT_TOKENS     NUMBER(10)    DEFAULT 0 NOT NULL,
    OUTPUT_TOKENS    NUMBER(10)    DEFAULT 0 NOT NULL,
    CREATED_AT       TIMESTAMP     NOT NULL,
    UPDATED_AT       TIMESTAMP     NOT NULL,
    CONSTRAINT PK_CHAT_MESSAGE                      PRIMARY KEY (CHAT_MESSAGE_ID),
    CONSTRAINT UK_CHAT_MESSAGE_SESSION_ID_SEQ_NO    UNIQUE (CHAT_SESSION_ID, SEQ_NO),
    CONSTRAINT CK_CHAT_MESSAGE_ROLE                 CHECK (ROLE IN ('USER', 'ASSISTANT'))
);
