-- 챗봇 전역 운영 설정. 돈이 드는 외부 API를 쓰므로 비용이 튀거나 키가 샜을 때 재배포 없이 즉시 끌 수 있어야 합니다.
-- 모델명, 타임아웃처럼 코드와 함께 움직이는 값은 설정 파일에 두고, 운영 중 급히 바꿀 값만 여기에 둡니다.
-- 행은 항상 하나입니다(CHECK로 강제).
CREATE TABLE CHAT_SETTING (
    CHAT_SETTING_ID           NUMBER(19)  DEFAULT 1 NOT NULL,
    ENABLED                   NUMBER(1)   DEFAULT 1 NOT NULL,   -- 0이면 전체 중단
    DEFAULT_DAILY_TOKEN_LIMIT NUMBER(12)  NOT NULL,             -- 계정별 설정이 없을 때 쓰는 기본 한도
    GLOBAL_DAILY_TOKEN_LIMIT  NUMBER(12)  NOT NULL,             -- 서비스 전체 일일 상한
    CREATED_AT                TIMESTAMP   NOT NULL,
    UPDATED_AT                TIMESTAMP   NOT NULL,
    CONSTRAINT PK_CHAT_SETTING            PRIMARY KEY (CHAT_SETTING_ID),
    CONSTRAINT CK_CHAT_SETTING_SINGLE_ROW CHECK (CHAT_SETTING_ID = 1),
    CONSTRAINT CK_CHAT_SETTING_ENABLED    CHECK (ENABLED IN (0, 1)),
    CONSTRAINT CK_CHAT_SETTING_LIMITS     CHECK (DEFAULT_DAILY_TOKEN_LIMIT >= 0 AND GLOBAL_DAILY_TOKEN_LIMIT >= 0)
);
