# 전체 구조

## 1. 컨테이너 구성

```
┌─────────── library-loan-service ───────────┐   ┌────────── ebook-commerce-service ──────────┐
│ library-user (8081)   library-admin (9081) │   │ ebook-user (8082)     ebook-admin (9082)   │
│ 회원 화면 + API        관리자 화면 + API     │   │ 회원 화면 + API        관리자 화면 + API     │
│        └──────── library-core ────────┘    │   │        └──────── ebook-core ────────┘      │
│   엔티티, Repository, 도메인 규칙, Flyway    │   │   엔티티, Repository, 도메인 규칙, Flyway    │
└──────────────────────┬─────────────────────┘   └──────────────────────┬─────────────────────┘
                       │ jdbc (LIBRARY 스키마)                            │ jdbc (EBOOK 스키마)
                       ▼                                                  ▼
┌──────────────────────────────────────────────────────────────────────────────────────────┐
│ oracle-db (1521)  gvenzl/oracle-free:23-slim                                             │
│   사용자 LIBRARY / 사용자 EBOOK  (스키마 분리, 서로 접근 불가)                              │
└──────────────────────────────────────────────────────────────────────────────────────────┘
```

- 서비스마다 회원용(user)과 관리자용(admin) 앱을 따로 띄웁니다. 포트, 배포, Security 설정이 서로 독립적입니다.
  관리자 포트(9xxx)는 운영 환경에서 사내망이나 VPN에서만 열어 외부 노출을 막는 것을 전제로 합니다.
- 같은 서비스의 user, admin 앱은 같은 스키마를 쓰고 core 모듈(엔티티, Repository, 마이그레이션)을 공유합니다.
- DB는 Oracle 인스턴스 하나에 스키마 2개로 분리합니다.
  MacBook에서 Oracle 컨테이너 2개는 메모리 부담이 크고, 실무에서도 "인스턴스는 공유하고 스키마로 격리"하는 경우가 많습니다.
- 두 서비스(library, ebook)는 서로 호출하지 않습니다. 회원 테이블도 각자 가집니다.
  (나중에 "통합 로그인" 또는 "대출 이력 기반 ebook 추천"을 확장 과제로 붙일 수 있습니다.)
- 로컬 개발 방식은 두 가지입니다.
  - 기능 개발: Oracle만 Docker로 띄우고(`docker compose up -d`) 앱은 IDE나 `bootRun`으로 실행합니다.
  - 전체 실행: `docker compose --profile app up -d --build`로 Oracle과 앱 4개를 모두 컨테이너로 띄웁니다.
- 앱 이미지는 서비스마다 Dockerfile 하나로 만들고 `MODULE` 빌드 인자로 user, admin을 구분합니다.
  - 멀티 스테이지 빌드: JDK로 jar를 만들고 실행 이미지에는 JRE와 jar 계층만 담습니다.
  - 계층형 jar: 의존성과 코드 계층을 나눠 코드만 바뀌면 의존성 계층을 재사용합니다.
  - 앱은 root가 아닌 `app` 사용자로 실행합니다.
- 컨테이너 안의 앱은 `docker` 프로필로 실행하고, DB 주소는 compose 서비스 이름(`oracle-db`), 비밀번호는 `.env` 값을 환경 변수로 받습니다.
  로컬 비밀번호가 든 `application-local.yml`은 jar와 이미지에 들어가지 않습니다.

## 2. 기술 스택 (두 서비스 공통)

| 구분 | 선택 | 비고 |
|---|---|---|
| Language | Java 21 (LTS) | 가상 스레드 사용 가능. Spring AI의 LLM 호출처럼 오래 기다리는 I/O에 유리 |
| Framework | Spring Boot 4.1.x, Spring MVC | Spring Framework 7, Hibernate 7, Jackson 3 |
| View | Thymeleaf + Thymeleaf Layout Dialect | 간단한 화면 |
| ORM | Spring Data JPA (Hibernate) | 복잡한 조회는 QueryDSL 또는 JPQL |
| DB | Oracle 23 Free (Docker) | ID는 SEQUENCE 전략 |
| Migration | Flyway (`spring-boot-starter-flyway`, `flyway-database-oracle`) | 서비스별 스키마에 각자 적용, `ddl-auto: validate` |
| Security | Spring Security | 화면: 세션(Form Login), API: JWT. SecurityFilterChain 2개로 분리 |
| Validation | spring-boot-starter-validation | |
| API 문서 | springdoc-openapi | /swagger-ui.html |
| Test | JUnit 5, Mockito, Testcontainers(Oracle `gvenzl/oracle-free:23-slim`) | Boot 4에서는 `@MockBean` 대신 `@MockitoBean` |
| Build | Gradle 9 (Groovy DSL), Gradle Wrapper | |
| CI | GitHub Actions | build + test |
| AI (예정) | Spring AI 2.0 | Boot 4.0~4.1 호환. Oracle 23의 VECTOR 타입을 벡터 저장소로 쓸 수 있음 |

## 3. 모듈과 패키지 구조 (두 서비스 공통)

Gradle 멀티모듈입니다. 모듈 이름의 `library`는 ebook 서비스에서 `ebook`으로 바뀝니다.

```
library-loan-service/
├── library-core/     일반 jar. 실행하지 않음
├── library-user/     회원 앱 (bootJar)  :8081
└── library-admin/    관리자 앱 (bootJar) :9081
```

의존 방향은 `user → core`, `admin → core` 뿐입니다. user와 admin은 서로 의존하지 않습니다.

```
library-core   com.ryu.library
├── global
│   ├── config             JpaConfig(Auditing), ClockConfig
│   ├── exception          ErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler(API 공통)
│   └── common             BaseTimeEntity, ApiResponse, PageResponse
├── member
│   ├── domain             Member, MemberRole, MemberStatus
│   ├── repository         MemberRepository
│   └── service            두 앱이 함께 쓰는 도메인 규칙만 (예: 회원 정지 판단)
├── book ...
└── loan ...
resources/application-core.yml, db/migration/
testFixtures/              TestcontainersConfiguration (user, admin 테스트에서도 사용)

library-user   com.ryu.library
├── LibraryUserApplication  메인 클래스를 com.ryu.library에 두어 core까지 스캔
└── user
    ├── global             SecurityConfig, 화면용 예외 처리
    ├── loan
    │   ├── controller     LoanViewController(화면), LoanApiController(/api)
    │   ├── service        LoanService (@Transactional, 흐름 조립, Response DTO 변환)
    │   └── dto            LoanCreateRequest, LoanResponse
    └── ...

library-admin  com.ryu.library
├── LibraryAdminApplication
└── admin
    ├── global             SecurityConfig(ADMIN만 허용), 화면용 예외 처리
    ├── book               controller, service, dto (도서·소장 자료 관리)
    └── ...
```

## 4. 개발 규칙

DB 설계, 엔티티, 계층, 예외, 트랜잭션, 테스트, Git 등 두 서비스 공통 규칙은 `01-rules.md`를 따릅니다.

## 5. 인증 흐름 (두 서비스 공통, user와 admin 앱이 각각 가짐)

```
브라우저 (Thymeleaf)                        외부 클라이언트 / Swagger
    │ POST /login (폼)                            │ POST /api/v1/auth/login (JSON)
    ▼                                             ▼
 세션 생성, JSESSIONID 쿠키                   AccessToken(30분) + RefreshToken(7일) 발급
    │                                             │ Authorization: Bearer ...
    ▼                                             ▼
 formLoginFilterChain (/**)                   apiFilterChain (/api/**, stateless)
```

구현 순서: 세션 Form Login 먼저 → API용 JWT 추가 → FilterChain 분리 커밋.

### API 토큰 (user 앱)
- Access Token: JWT(HS256), 30분. `sub`에 회원 ID, `roles`에 권한만 넣습니다. 서명과 만료 검증은 Spring Security 리소스 서버(`oauth2ResourceServer().jwt()`)가 합니다.
- Refresh Token: 무작위 32바이트 문자열, 7일. DB(REFRESH_TOKEN)에는 SHA-256 해시만 저장합니다.
  - 갱신할 때마다 새 토큰으로 바꾸고 쓴 토큰은 폐기합니다(토큰 교체).
  - 폐기된 토큰이 다시 오면 탈취로 보고 그 회원의 토큰을 모두 폐기합니다.
- 로그아웃은 Refresh Token만 폐기합니다. 이미 발급된 Access Token은 만료될 때까지 유효합니다(짧은 만료 시간으로 위험을 줄임).
- 서명 키는 환경 변수 `LIBRARY_JWT_SECRET`(32바이트 이상)로만 넣습니다. 없거나 짧으면 앱이 시작되지 않습니다.
- 화면 세션 쿠키로는 API가 인증되지 않습니다. API 체인은 세션을 읽지 않습니다.

- user 앱과 admin 앱은 세션을 공유하지 않습니다. 관리자는 admin 앱에서 따로 로그인합니다.
- 계정은 같은 MEMBER 테이블을 쓰고, admin 앱은 ROLE이 ADMIN인 회원만 로그인을 허용합니다. 일반 회원 아이디는 "없는 계정"과 똑같이 거부합니다.
- 관리자 화면 로그인 URL은 `/admin/login`, 회원 화면은 `/login`입니다. 로그인 성공 처리와 실패 처리는 Spring Security 필터가 맡습니다.
- API 인증 실패는 로그인 화면으로 보내지 않고 `ApiResponse` JSON(401 `C006`, 403 `C007`)으로 응답합니다.
