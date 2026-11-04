# ebook-commerce-service

전자책을 검색하고 장바구니에 담아 결제한 뒤 내 서재에서 읽는 ebook 커머스 서비스입니다.
주문, 결제, 소유권(내 서재)이라는 커머스 핵심 흐름을 Spring Boot로 정확하게 구현하는 학습용 프로젝트입니다.

- 상태: 프로젝트 뼈대 완료 (멀티모듈, 공통 설정, 예외 처리, Flyway). 도메인 구현 전
- 문서: `docs/01-erd.md` (테이블 설계), `docs/02-screen-flow.md` (화면 흐름과 URL)
- 스키마: `ebook-core/src/main/resources/db/migration/` (Flyway). `V1__init.sql` 테이블, `V2__insert_default_categories.sql` 기본 분류
- 관리자 계정은 마이그레이션에 넣지 않습니다. 도서 대출 서비스처럼 admin 앱이 시작할 때 환경 변수로 한 번 만듭니다(회원 기능을 만들 때 추가).

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
