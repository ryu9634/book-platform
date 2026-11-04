# library-loan-service

도서관 회원이 종이책을 검색하고 대출, 반납, 연장, 예약하는 도서 대출 관리 서비스입니다.
Spring Boot 기본기를 정확하게 쓰는 것을 목표로 한 학습용 프로젝트입니다.

- 상태: 구현 완료

## 기능

| 앱 | 기능 |
|---|---|
| user (8081) | 회원가입·로그인(세션), 도서 검색·상세, 대출(동시 대출 방지), 반납, 연장(1회 7일), 예약(대기 순번, 수령 기한 3일), 내 대출·이력·예약, 내 정보·비밀번호 변경 |
| user API | JWT 로그인·갱신·로그아웃, 도서, 대출·반납·연장, 예약, 인기 도서(ebook 서비스가 호출) — `/swagger-ui.html` |
| admin (9081) | 대시보드, 도서 등록(국립중앙도서관 API로 서지 정보 채움)·수정, 소장 자료(바코드 자동 생성, 분실·폐기), 대출 현황·연체 현황·데스크 반납, 회원 정지·해제·권한, 분류 관리 |
| admin 정기 작업 | 매일 00:10 연체 전환, 10분마다 예약 수령 기한 만료 |
- 문서: `docs/01-erd.md` (테이블 설계), `docs/02-screen-flow.md` (화면 흐름과 URL)
- 스키마: `library-core/src/main/resources/db/migration/` (Flyway). `V1__init.sql` 테이블, `V2__insert_default_categories.sql` 기본 분류
- 관리자 계정은 마이그레이션에 넣지 않습니다. admin 앱이 시작할 때 환경 변수 `LIBRARY_ADMIN_LOGIN_ID`, `LIBRARY_ADMIN_PASSWORD`(루트 `.env`)로 한 번 만듭니다. 값이 비어 있으면 만들지 않습니다.

## 모듈

| 모듈 | 역할 | 포트 |
|---|---|---|
| `library-core` | 엔티티, Repository, 두 앱 공통 도메인 규칙, 공통 설정, Flyway 마이그레이션 | 실행 안 함 |
| `library-user` | 회원용 화면과 API | 8081 |
| `library-admin` | 관리자용 화면과 API | 9081 |

## 로컬 실행

```bash
# 1. Oracle 실행 (book-platform 루트에서). 처음 한 번은 .env를 만들고 비밀번호를 직접 정해서 넣습니다.
cp .env.example .env
docker compose up -d

# 2. 앱마다 로컬 설정 파일을 만들고 password에 .env의 LIBRARY_DB_PASSWORD 값을 넣습니다. (git에 올리지 않음)
cp library-user/src/main/resources/application-local.yml.example library-user/src/main/resources/application-local.yml
cp library-admin/src/main/resources/application-local.yml.example library-admin/src/main/resources/application-local.yml

# 3. 실행. 먼저 뜨는 앱이 Flyway 마이그레이션을 적용합니다.
./gradlew :library-user:bootRun     # http://localhost:8081
./gradlew :library-admin:bootRun    # http://localhost:9081
```

로컬 Oracle 없이 띄워 보려면 `./gradlew :library-user:bootTestRun`을 씁니다. Testcontainers가 Oracle 컨테이너를 따로 띄웁니다.

테스트도 Testcontainers를 쓰므로 Docker만 켜져 있으면 됩니다: `./gradlew test`

## Docker로 실행

이 서비스의 user, admin 앱을 컨테이너로 띄우려면 루트에서 실행합니다.

```bash
docker compose --profile app up -d --build library-user library-admin
```

이미지는 `Dockerfile` 하나로 만들고 `MODULE` 빌드 인자로 앱을 고릅니다. 직접 빌드할 때는 이 폴더에서 다음 명령을 씁니다.

```bash
docker build --build-arg MODULE=library-user -t book-platform/library-user .
```
