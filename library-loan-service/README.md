# library-loan-service

도서관 회원이 종이책을 검색하고 대출, 반납, 연장, 예약하는 도서 대출 관리 서비스입니다.
Spring Boot 기본기를 정확하게 쓰는 것을 목표로 한 학습용 프로젝트입니다.

- 상태: 프로젝트 뼈대 완료 (멀티모듈, 공통 설정, 예외 처리, Flyway). 도메인 구현 전
- 문서: `docs/01-erd.md` (테이블 설계), `docs/02-screen-flow.md` (화면 흐름과 URL)
- 스키마: `library-core/src/main/resources/db/migration/` (Flyway). `V1__init.sql` 테이블, `V2__insert_default_categories.sql` 기본 분류
- 관리자 계정은 마이그레이션에 넣지 않습니다. 비밀번호 해시가 저장소에 남지 않도록 회원 기능을 만들 때 admin 앱의 local 프로필 초기화 코드로 만듭니다.

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
