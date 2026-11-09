# ebook-commerce-service

전자책을 검색하고 장바구니에 담아 결제한 뒤 내 서재에서 읽는 ebook 커머스 서비스입니다.
주문, 결제, 소유권(내 서재)이라는 커머스 핵심 흐름을 Spring Boot로 정확하게 구현하는 학습용 프로젝트입니다.

- 상태: 구현 완료

## 기능

| 앱 | 기능 |
|---|---|
| user (8082) | 회원가입(장바구니 생성)·로그인, 전자책 목록(검색·분류·정렬)·상세, 장바구니, 주문서·결제(Mock PG 승인·실패·취소)·주문 완료, 주문 내역, 내 서재·뷰어(이어 읽기), 부분 환불(7일, 미열람), 리뷰, 메인의 "도서관에서 많이 빌린 책" 추천, AI 도서 추천 챗봇 |
| user API | JWT 로그인, 전자책, 리뷰, 장바구니, 주문·결제·환불, 서재·본문, AI 도서 추천 — `/swagger-ui.html` |
| admin (9082) | 대시보드, 전자책 등록·수정·판매 상태·본문 파일 업로드, 주문 관리·강제 환불, 회원 탈퇴 처리, 매출 요약(월별·분류별·상위 전자책), 챗봇 프롬프트·계정별 설정·토큰 사용량 |
| admin 정기 작업 | 10분마다 30분 넘은 미결제 주문 취소 |
- 문서: `docs/01-erd.md` (테이블 설계), `docs/02-screen-flow.md` (화면 흐름과 URL)
- 스키마: `ebook-core/src/main/resources/db/migration/` (Flyway). `V1__init.sql` 테이블, `V2__insert_default_categories.sql` 기본 분류, `V4`~`V12` 챗봇 테이블과 기본 프롬프트
- 관리자 계정은 마이그레이션에 넣지 않습니다. admin 앱이 시작할 때 환경 변수 `EBOOK_ADMIN_LOGIN_ID`, `EBOOK_ADMIN_PASSWORD`(루트 `.env`)로 한 번 만듭니다. 값이 비어 있으면 만들지 않습니다.

## AI 도서 추천 챗봇

회원 앱의 `/chat`에서 대화하며 전자책을 추천받습니다. Spring AI 2.0으로 Anthropic Claude(기본 `claude-haiku-5-5`)를 부릅니다.

| 주제 | 방식 |
|---|---|
| 없는 책을 지어내지 않게 | 도구 호출(`@Tool`)로 전자책 검색·분류·보유 도서를 열어 주고, 검색 결과에 없는 책은 추천하지 못하게 프롬프트로 묶습니다. 후보는 판매 중(ON_SALE)인 책만 |
| 대화 이어가기 | 회원당 진행 중 세션 1개(`CHAT_SESSION`). 최근 메시지 N개를 다음 요청에 다시 실어 보냅니다. Spring AI의 ChatMemory 대신 우리 테이블을 그대로 씁니다 |
| 시스템 프롬프트 | `CHAT_PROMPT` 테이블. 관리자가 재배포 없이 고치고, 이전 본문은 `CHAT_PROMPT_HISTORY`에 남습니다 |
| 비용 제한 | 계정별 사용 여부와 일일 토큰 한도, 서비스 전체 일일 한도, 전체 중단 스위치, 질문 길이·연타·검색 횟수·출력 토큰 상한 |
| 사용량 집계 | 호출 뒤 `CHAT_TOKEN_USAGE_DAILY`에 더합니다(MERGE 한 문장). 날짜별 행이라 초기화 배치가 없습니다 |
| 개인정보 | 프롬프트에 회원 이름·이메일·아이디를 넣지 않습니다. 대화 내용은 로그에 남기지 않고, 화면에 외부 전송 사실을 알립니다 |
| 추천 도서 | 답변에 제목이 나온 전자책을 `CHAT_MESSAGE_EBOOK`에 묶어 둡니다. 그래서 새로 고쳐도, API로 다시 읽어도 같은 카드가 나옵니다 |
| 답변 스트리밍 | `/chat/stream`이 답변을 글자 단위로 보냅니다(SSE). 질문이 접속 로그에 남지 않게 POST로 받으므로 `EventSource` 대신 화면에서 `fetch`로 읽습니다. 스크립트가 없거나 `ebook.chat.stream-enabled`를 끄면 폼 전송으로 그대로 동작합니다 |
| 모델 교체 | `ChatCompletionPort` 인터페이스 하나만 구현하면 바뀝니다. 테스트는 이 포트를 대역으로 바꿔 실제 API를 부르지 않습니다 |

화면은 `/chat`, API는 `/api/v1/chat`입니다. 둘 다 같은 `ChatService`를 쓰므로 한도와 세션 규칙이 똑같이 적용됩니다.
스트리밍은 가상 스레드(Java 21)에서 돌려 기다리는 동안 플랫폼 스레드를 잡지 않습니다.

API 키는 환경 변수 `EBOOK_ANTHROPIC_API_KEY`(루트 `.env`)로만 넣습니다.
비어 있으면 챗봇 화면에서만 오류가 나고 나머지 기능은 그대로 뜹니다.
모델과 한도 관련 설정은 `ebook.chat.*`(회원 앱 `application.yml`), 토큰 단가는 `ebook.chat-pricing.*`(`application-core.yml`)에 있습니다.
모델을 바꾸면 단가도 함께 바꿉니다.

## 모듈

| 모듈 | 역할 | 포트 |
|---|---|---|
| `ebook-core` | 엔티티, Repository, 두 앱 공통 도메인 규칙, 공통 설정, Flyway 마이그레이션 | 실행 안 함 |
| `ebook-user` | 회원용 화면과 API | 8082 |
| `ebook-admin` | 관리자용 화면과 API | 9082 |

## 로컬 실행

```bash
# 1. Oracle 실행 (book-platform 루트에서). 처음 한 번은 .env를 만들고 비밀번호를 직접 정해서 넣습니다.
cp .env.example .env
docker compose up -d

# 2. 앱마다 로컬 설정 파일을 만들고 password에 .env의 EBOOK_DB_PASSWORD 값을 넣습니다. (git에 올리지 않음)
cp ebook-user/src/main/resources/application-local.yml.example ebook-user/src/main/resources/application-local.yml
cp ebook-admin/src/main/resources/application-local.yml.example ebook-admin/src/main/resources/application-local.yml

# 3. 실행. 먼저 뜨는 앱이 Flyway 마이그레이션을 적용합니다.
./gradlew :ebook-user:bootRun     # http://localhost:8082
./gradlew :ebook-admin:bootRun    # http://localhost:9082
```

로컬 Oracle 없이 띄워 보려면 `./gradlew :ebook-user:bootTestRun`을 씁니다. Testcontainers가 Oracle 컨테이너를 따로 띄웁니다.

테스트도 Testcontainers를 쓰므로 Docker만 켜져 있으면 됩니다: `./gradlew test`

## Docker로 실행

이 서비스의 user, admin 앱을 컨테이너로 띄우려면 루트에서 실행합니다.

```bash
docker compose --profile app up -d --build ebook-user ebook-admin
```

이미지는 `Dockerfile` 하나로 만들고 `MODULE` 빌드 인자로 앱을 고릅니다. 직접 빌드할 때는 이 폴더에서 다음 명령을 씁니다.

```bash
docker build --build-arg MODULE=ebook-user -t book-platform/ebook-user .
```
