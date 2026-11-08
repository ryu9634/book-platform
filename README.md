# book-platform

도서를 주제로 한 Spring Boot 학습용 프로젝트입니다. 독립 서비스 2개를 모노레포로 관리하고, 구현은 모두 끝났습니다.

| 서비스 | 폴더 | 설명 | 회원 포트 | 관리자 포트 |
|---|---|---|---|---|
| 도서 대출 관리 | [`library-loan-service/`](library-loan-service) | 회원이 종이책을 대출, 반납, 예약하는 도서관 시스템 | 8081 | 9081 |
| ebook 커머스 | [`ebook-commerce-service/`](ebook-commerce-service) | 회원이 전자책을 결제하고 내 서재에서 읽는 쇼핑몰 | 8082 | 9082 |

두 서비스는 DB와 회원 테이블을 따로 가집니다. 유일한 호출은 ebook 메인이 도서관의 인기 도서 API를 불러 같은 ISBN의 전자책을 추천하는 것 하나입니다.

서비스마다 Gradle 멀티모듈(`core` 도메인, `user` 회원 앱, `admin` 관리자 앱)이고, 의존 방향은 `user → core`, `admin → core` 뿐입니다.
기능 목록과 IDE 실행 방법은 각 서비스 README에 있습니다.

## 실행

JDK 21과 Docker가 필요합니다. 처음 한 번은 `.env`를 만들고 비밀번호를 직접 넣습니다.

```bash
cp .env.example .env
```

```bash
docker compose up -d                          # Oracle만 실행 (앱은 IDE에서 개발할 때)
docker compose --profile app up -d --build    # Oracle + 앱 4개 실행
docker compose --profile app down             # 전체 종료 (데이터 유지, 삭제하려면 -v)
```

| 앱 | 주소 |
|---|---|
| 도서 대출 회원 / 관리자 | http://localhost:8081 / http://localhost:9081 |
| ebook 회원 / 관리자 | http://localhost:8082 / http://localhost:9082 |
| API 문서 (회원 앱) | http://localhost:8081/swagger-ui.html, http://localhost:8082/swagger-ui.html |

관리자 계정은 `.env`의 `LIBRARY_ADMIN_*`, `EBOOK_ADMIN_*` 값으로 admin 앱이 처음 시작할 때 만들어집니다.
테스트는 서비스 폴더에서 `./gradlew test`로 돌립니다. Testcontainers가 Oracle을 띄우므로 Docker만 켜져 있으면 됩니다.

## 기술 스택

Java 21 · Spring Boot 4.1 (MVC) · Thymeleaf · Spring Data JPA + Oracle 23 Free + Flyway · Spring Security(화면은 세션 + CSRF, API는 JWT) · springdoc-openapi · JUnit 5 + Testcontainers · Gradle 9 · GitHub Actions

버전과 선택 이유는 [`docs/00-architecture.md`](docs/00-architecture.md) 2절에 있습니다.

## 설계 포인트

| 주제 | 내용 | 위치 |
|---|---|---|
| FK 없는 설계 | 참조 무결성은 서비스에서 확인, 참조 컬럼마다 인덱스 | [`docs/01-rules.md`](docs/01-rules.md) 1절 |
| 동시성 | 남은 한 권 동시 대출(`@Version`), 같은 주문 동시 결제(비관적 락), 중복 생성(UNIQUE 제약) | 각 서비스 core 테스트 |
| 인증 | 화면은 세션 + CSRF, API는 JWT (Access 30분 + Refresh 7일 교체·탈취 감지) | 각 서비스 user `global/security` |
| 결제·환불 | PG 호출은 트랜잭션 밖에서, 반영은 주문을 잠근 뒤, 실패하면 보상(PG 취소) | ebook core `payment`, `order` |
| 외부 API | 국립중앙도서관 API: 타임아웃, 200 + 본문 오류 확인, 인증키를 로그에 남기지 않음 | library admin `book.client` |
| AI 도서 추천 | Spring AI + Claude. 도구 호출로 실제 판매 중인 전자책만 추천, 계정별 일일 토큰 한도와 전체 중단 스위치 | ebook core `chat`, ebook user `chat`, ebook admin `chat` |
| 서비스 간 통신 | HTTP Interface, 짧은 타임아웃, 캐시, fallback | [`docs/00-architecture.md`](docs/00-architecture.md) 6절 |

## 문서

- [`docs/00-architecture.md`](docs/00-architecture.md) : 컨테이너 구성, 기술 스택, 패키지 구조, 인증 흐름, 서비스 간 통신
- [`docs/01-rules.md`](docs/01-rules.md) : 공통 개발 규칙 (DB 설계, 엔티티, 계층, 예외, 트랜잭션, 테스트, Git)
- [`library-loan-service/docs/`](library-loan-service/docs), [`ebook-commerce-service/docs/`](ebook-commerce-service/docs) : 서비스별 ERD, 화면 흐름
- 각 서비스 `*-core/src/main/resources/db/migration/` : Flyway 마이그레이션 (스키마 기준)
