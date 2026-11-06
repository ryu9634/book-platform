# 개발 규칙

두 서비스(library-loan-service, ebook-commerce-service)에 공통으로 적용하는 규칙입니다.
서비스 하나에만 해당하는 내용은 각 서비스의 `docs/`에 적습니다.
규칙을 예외로 두어야 할 때는 코드에 주석으로 이유를 남기고, 필요하면 이 문서도 고칩니다.

"처음부터" 표시가 있는 규칙은 프로젝트 뼈대 단계부터 강제하고, 나머지는 해당 기능을 만들 때 적용합니다.

---

## 1. DB 설계 (처음부터)

### 1-1. FK 제약은 쓰지 않는다
- DDL에 `FOREIGN KEY ... REFERENCES ...` 제약을 만들지 않습니다.
- PK, UNIQUE, CHECK, NOT NULL 제약은 적극적으로 씁니다. FK만 뺍니다.
- 다른 테이블을 가리키는 컬럼은 `{참조 테이블}_ID NUMBER(19)` 형식으로 이름을 붙입니다. (예: `MEMBER_ID`, `EBOOK_ID`)
  자기 테이블을 가리키는 컬럼만 `PARENT_ID`처럼 역할 이름을 씁니다.
- **참조 컬럼에는 반드시 인덱스를 만듭니다.** 조회 조건이 자주 함께 쓰이면 `(MEMBER_ID, STATUS)` 같은 복합 인덱스로 만듭니다.
  참조 컬럼이 UNIQUE 제약이나 복합 인덱스의 첫 번째 컬럼이면 그 인덱스로 충분하므로 따로 만들지 않습니다.
- ERD에는 FK가 없어도 논리적 관계(1:N, N:1)를 표시합니다. ERD 위에 모든 관계가 논리 참조라고 적고, 참조 컬럼 설명에 `→ 대상 테이블`을 적습니다.
- 마이그레이션 SQL의 참조 컬럼 옆에는 `-- 논리 참조: 대상 테이블` 주석을 붙입니다.

### 1-2. FK 대신 애플리케이션이 무결성을 지킨다
- **저장할 때**: 참조 대상이 실제로 있는지 조회해서 확인한 뒤 저장합니다. 없으면 `XXX_NOT_FOUND` 예외를 던집니다.
- **삭제할 때**: 물리 삭제 대신 상태 변경(소프트 삭제)을 기본으로 합니다. 부모가 지워져 자식이 고아가 되는 일을 막기 위해서입니다.
- **물리 삭제가 꼭 필요할 때**: 서비스 메서드 하나(트랜잭션 하나) 안에서 자식을 먼저 지우고 부모를 지웁니다.
- **조회할 때**: 참조 대상이 소프트 삭제된 상태일 수 있다는 점을 생각하고 화면과 응답을 만듭니다. (예: 탈퇴한 회원의 주문 내역)

### 1-3. 테이블, 컬럼
- 테이블명과 컬럼명은 대문자 SNAKE_CASE. Oracle 예약어(USER, ORDER, DATE, COMMENT 등)는 피합니다. (회원은 MEMBER, 주문은 ORDERS)
- 제약과 인덱스 이름은 `PK_{테이블}`, `UK_{테이블}_{컬럼}`, `CK_{테이블}_{컬럼}`, `IDX_{테이블}_{컬럼}`.
- PK는 모두 `{테이블명}_ID NUMBER(19)`, 시퀀스는 `{테이블명}_SEQ`(INCREMENT BY 50).
  JPA는 `@SequenceGenerator(allocationSize = 50)`으로 insert 시 시퀀스 호출 횟수를 줄입니다.
- PK에 UUID를 쓰지 않습니다. 이유는 네 가지입니다.
  - Oracle에는 UUID 타입이 없어 `RAW(16)`이나 `VARCHAR2(36)`으로 저장해야 합니다. 조회할 때 읽기 어렵거나 저장 공간이 커집니다.
  - 랜덤 UUID는 B-tree 인덱스의 아무 위치에나 들어가 인덱스 분할과 단편화가 생깁니다.
    FK를 쓰지 않는 대신 참조 컬럼마다 인덱스를 만들기 때문에, 키가 커지면 이 인덱스들도 모두 커집니다.
  - JPA에서 ID를 미리 만들어 넣으면 `save()`가 `merge()`로 동작해 SELECT가 한 번 더 나갑니다.
  - ID를 추측해 남의 데이터에 접근하는 문제는 서비스에서 로그인 회원과 데이터 소유자를 비교해 막습니다.
  외부에 숫자 ID를 숨겨야 하는 테이블이 생기면 PK는 그대로 두고 노출용 `{테이블}_KEY VARCHAR2(36) UNIQUE` 컬럼만 추가합니다.
- 모든 테이블에 `CREATED_AT`, `UPDATED_AT TIMESTAMP NOT NULL` (BaseTimeEntity + JPA Auditing).
- 문자열은 `VARCHAR2(n CHAR)`로 선언해 한글 바이트 수 문제를 피합니다.
- 상태 값은 `VARCHAR2(20)` + CHECK 제약, JPA에서는 `@Enumerated(EnumType.STRING)`.
- 불리언은 `NUMBER(1)` (0/1), JPA에서는 `boolean` + `@Convert(converter = NumericBooleanConverter.class)`.
  Hibernate 7은 Oracle 23에서 `boolean`을 Oracle의 새 `BOOLEAN` 타입으로 기대하므로, 변환기를 붙이지 않으면 스키마 검증(`validate`)이 실패합니다.
- 금액은 `NUMBER(12)` 정수 원 단위.

### 1-4. 스키마 관리 (Flyway)
- 스키마는 Flyway로 관리합니다. 각 서비스 core 모듈의 `src/main/resources/db/migration/` 아래 마이그레이션 파일이 스키마의 기준입니다.
- 의존성은 `flyway-core`와 `flyway-database-oracle` 두 개를 추가합니다. (Flyway 10부터 DB별 모듈이 분리됨)
- 파일 이름은 `V{번호}__{설명}.sql` 형식입니다. 설명은 영어 snake_case. (예: `V1__init.sql`, `V2__add_loan_extend_count.sql`)
- **이미 적용된 마이그레이션 파일은 절대 고치지 않습니다.** 체크섬이 달라져 애플리케이션이 뜨지 않습니다. 변경은 항상 새 파일로 추가합니다.
- Oracle DDL은 롤백되지 않으므로 마이그레이션 파일 하나에는 변경 하나만 담습니다. 중간에 실패하면 일부만 적용된 상태로 남기 때문입니다.
- 테이블을 바꾸면 마이그레이션 파일과 ERD(`docs/01-erd.md`)를 같은 커밋에서 함께 고칩니다.
- 마이그레이션에는 비밀번호 해시 같은 민감한 값을 넣지 않습니다. 기본 분류처럼 공개돼도 되는 기준 데이터만 넣습니다.
- `spring.jpa.hibernate.ddl-auto`는 `validate`. 테이블은 Flyway가 만들고 Hibernate는 엔티티와 테이블이 맞는지만 확인합니다. 테스트도 같은 마이그레이션으로 스키마를 만듭니다.
- DB 사용자(스키마) 생성은 Flyway가 아니라 `docker/oracle/init/` 스크립트가 맡습니다. Flyway는 각 서비스의 DB 사용자로 접속해 그 스키마 안의 테이블만 관리합니다.
- `flyway clean`은 쓰지 않습니다. 로컬 DB를 처음부터 다시 만들고 싶으면 `docker compose down -v`로 볼륨을 지우고 다시 띄웁니다.

---

## 2. 모듈과 계층 (처음부터)

### 2-1. 모듈 (core / user / admin)
- 의존 방향은 `user → core`, `admin → core` 뿐입니다. user와 admin은 서로 의존하지 않고, core는 어느 앱도 모릅니다.
- 어디에 둘지는 아래 표를 따릅니다.

| 무엇 | 위치 |
|---|---|
| 엔티티, enum, Repository, Flyway 마이그레이션 | core |
| user와 admin이 **함께** 쓰는 비즈니스 규칙 (예: 반납 처리, 연체 판단) | core의 `{도메인}.service` |
| 공통 설정, 예외(`ErrorCode`, `BusinessException`), API 예외 처리, `ApiResponse` | core의 `global` |
| Controller, Request/Response DTO, 한쪽 앱에서만 쓰는 Service | 각 앱의 `user.{도메인}`, `admin.{도메인}` |
| Security 설정, 화면용 예외 처리, Thymeleaf 템플릿 | 각 앱 |

- 처음에는 한 앱에서만 쓰는 로직을 그 앱에 둡니다. 다른 앱에서도 필요해지면 그때 core로 옮깁니다. 복사해서 두 군데에 두지 않습니다.
- core에 Controller나 화면 관련 코드를 두지 않습니다. 예외는 두 앱이 똑같이 쓰는 웹 공통 처리뿐입니다: API 예외 처리(`GlobalExceptionHandler`), API 인증 실패 JSON 응답(`ApiSecurityErrorHandler`), 비밀번호 암호화 설정(`PasswordEncoderConfig`).
  core는 이를 위해 Spring MVC와 Security를 `compileOnly`로만 참조하고, 실제 라이브러리는 앱이 제공합니다.
- 메인 클래스는 두 앱 모두 `com.ryu.{서비스}` 패키지에 둡니다. 그래야 core의 엔티티와 설정까지 스캔됩니다.

### 2-2. 계층별 책임

| 계층 | 하는 일 | 하지 않는 일 |
|---|---|---|
| Controller | 요청 바인딩, `@Valid` 검증, Service 호출, `ApiResponse`로 감싸기 | 비즈니스 판단(if문), Repository 직접 호출, 엔티티 다루기 |
| Service | 비즈니스 규칙, 트랜잭션 경계, 흐름 조립, 참조 대상 존재 확인, 엔티티 → Response DTO 변환 | Security 객체 참조, HTTP 관련 타입 참조 |
| Repository | Spring Data JPA 인터페이스, 쿼리 | 비즈니스 규칙 |
| Domain (Entity) | 상태 변경 메서드, 자기 상태에 대한 규칙 검증 | 다른 엔티티 조회, Service 호출 |

- 의존 방향은 Controller → Service → Repository 한 방향입니다.
- 다른 도메인이 필요하면 Repository가 아니라 그 도메인의 Service를 호출합니다. 순환 참조가 생기면 Repository 직접 조회를 허용합니다.
- Repository 메서드명 쿼리는 조건 2개까지. 그 이상은 `@Query`(JPQL) 또는 QueryDSL.
- 상태 변경은 엔티티 메서드로 합니다. `loan.extend(days)`, `order.pay(payment)`. 규칙 위반 시 엔티티가 직접 예외를 던지고 Service는 흐름만 조립합니다.

---

## 3. 엔티티 (처음부터)

### 3-1. 연관관계 매핑
- **같은 애그리거트 안**(부모 없이 존재할 수 없는 자식, 예: ORDERS → ORDER_ITEM, CART → CART_ITEM)은 객체 참조(`@ManyToOne`, `@OneToMany`)를 씁니다.
- **다른 애그리거트**(예: ORDERS → MEMBER, LOAN → BOOK)는 ID 값(`Long memberId`)으로만 참조합니다.
- 객체 참조를 쓸 때는 Hibernate가 FK를 만들지 않도록 반드시 아래처럼 선언합니다.
  ```java
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "ORDER_ID", nullable = false,
              foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
  private Orders order;
  ```
- 모든 `@ManyToOne`, `@OneToOne`은 `LAZY`. `EAGER`는 쓰지 않습니다.
- 양방향 연관관계는 조회 편의를 위해 꼭 필요할 때만 추가하고, 추가하면 연관관계 편의 메서드를 함께 작성합니다.
- `cascade`, `orphanRemoval`은 부모 없이 존재할 수 없는 자식에만 씁니다.
  허용: ORDERS → ORDER_ITEM, CART → CART_ITEM. 금지: LOAN, BOOKSHELF처럼 독립 생명주기를 가진 것.

### 3-2. 엔티티 작성
- `@NoArgsConstructor(access = AccessLevel.PROTECTED)`. 생성은 정적 팩토리(`Member.create(...)`) 또는 `@Builder` 중 하나만. 둘 다 쓰지 않습니다.
- `@Setter`, `@Data` 금지. `@ToString`은 연관관계 필드 제외(`exclude` 또는 `@ToString.Exclude`).
- `equals/hashCode`는 ID 기반으로만, 그것도 `Set`에 담는 등 꼭 필요할 때만 작성합니다.
- 모든 엔티티는 `BaseTimeEntity`(createdAt, updatedAt)를 상속합니다.

---

## 4. DTO (처음부터)

- 요청과 응답 DTO는 `record`로 작성합니다. 이름은 `XxxCreateRequest`, `XxxResponse` 형식.
- Request/Response DTO는 각 앱 모듈에 둡니다. 같은 이름이어도 user와 admin의 DTO는 따로 만듭니다(관리자에게만 보이는 필드가 다르기 때문).
- 응답 변환은 `XxxResponse.from(entity)` 정적 메서드이고, 트랜잭션 안(Service)에서 호출합니다. `open-in-view: false`라서 Controller에서 지연 로딩을 건드리면 예외가 납니다. 요청 → 엔티티 변환은 `request.toEntity()`를 만들지 않고 Service에서 생성합니다(엔티티 생성 규칙이 Service 쪽에 있어야 하므로).
- 화면용과 API용 DTO는 처음엔 공유하고, 달라지는 순간 분리합니다.
- 엔티티를 Controller 응답이나 Thymeleaf 모델에 직접 넣지 않습니다. 지키기 가장 어려운 규칙이므로 PR 체크리스트에 포함합니다.

---

## 5. 예외 (처음부터)

- `BusinessException(ErrorCode)` 하나를 루트로 두고, 도메인별 하위 예외 클래스는 만들지 않습니다. 구분은 `ErrorCode` enum으로 합니다.
- `ErrorCode`는 `HttpStatus`, `code`, `message`를 가집니다. 메시지는 사용자에게 보여줄 문장이며, 파라미터가 필요하면 `message(Object... args)` 메서드로 포맷합니다.
  (예: `LOAN_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "L001", "대출 가능 권수를 초과했습니다.")`)
- `GlobalExceptionHandler`가 처리하는 것:
  - `BusinessException` → ErrorCode의 상태와 메시지
  - `MethodArgumentNotValidException`, `BindException` → 400, 필드 오류 목록
  - `OptimisticLockingFailureException` → 409
  - `DataIntegrityViolationException` → 409 (유니크 제약 위반을 비즈니스 메시지로 변환)
  - 깨진 JSON, 타입 불일치, 필수 파라미터 누락 → 400 / 지원하지 않는 Content-Type → 415
  - 없는 경로(404)와 지원하지 않는 메서드(405)는 컨트롤러를 정하기 전에 나는 예외라 `annotations = RestController.class` 범위의 핸들러까지 오지 않습니다. Spring 기본 처리로 상태 코드만 맞게 나갑니다.
  - 그 외 `Exception` → 500, 로그만 남기고 메시지는 숨김
- 화면 요청과 API 요청의 에러 응답이 다르므로 `@RestControllerAdvice(annotations = RestController.class)`와 화면용 `@ControllerAdvice`를 분리합니다.
- 예외를 catch해서 삼키지 않습니다. catch하면 반드시 로그를 남기거나 다시 던집니다.
- `Optional.get()`은 쓰지 않고 `orElseThrow(() -> new BusinessException(...))`를 씁니다.

---

## 6. 트랜잭션 (처음부터)

- Service 클래스 레벨에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`을 다시 선언합니다.
- private 메서드나 같은 클래스 내부 호출에 `@Transactional`을 붙이지 않습니다(프록시를 거치지 않아 동작하지 않음). 이 내용은 README의 "배운 점"에 한 번 정리합니다.
- 외부 호출(PG, 메일 발송)은 트랜잭션 밖에서 합니다. 커밋 이후에 실행할 동작은 `@TransactionalEventListener(phase = AFTER_COMMIT)`.

### 외부 API 호출
- 클라이언트는 그 API를 쓰는 앱 모듈에 `{도메인}.client` 패키지로 둡니다. (예: 국립중앙도서관 → admin의 `book.client`)
- 연결 타임아웃과 응답 타임아웃을 반드시 설정합니다. 값은 설정 파일에 둡니다.
- 외부 API의 오류는 `EXTERNAL_API_UNAVAILABLE`(503)처럼 우리 에러 코드로 바꿉니다. 외부 응답 형식이 우리 코드 밖으로 새어 나가지 않게 합니다.
- HTTP 상태만 믿지 않습니다. 결과 없음이나 오류를 200 + 본문(예: 공공데이터포털 `header.resultCode`)으로 알려 주는 API는 본문을 확인합니다.
- 명세와 실제 응답이 다를 수 있습니다. 실제 키로 한 번 호출해 응답 형식을 확인하고, 그 응답을 줄여서 테스트 데이터로 씁니다.
- 인증키가 URL에 들어가는 API는 예외 메시지(URL 포함)를 그대로 로그에 남기지 않습니다. 예외 종류만 남깁니다.
- 인증키는 환경 변수로만 넣습니다. 비어 있으면 외부를 호출하지 않고 "설정되지 않음" 오류를 냅니다.
- 테스트는 WireMock으로 정상, 결과 없음, 인증키 오류, 5xx, 지연(타임아웃)을 재현합니다. 실제 외부 API는 자동 테스트에서 부르지 않습니다.

---

## 7. 조회와 성능

- `spring.jpa.open-in-view: false`. 지연 로딩은 Service 계층 안에서 끝냅니다.
- 목록 조회는 항상 `Pageable`. 기본 20건, 최대 100건(`@PageableDefault(size = 20)`, Service에서 상한 클램프).
- 연관 엔티티가 필요한 목록은 fetch join 또는 `@EntityGraph`. 페이징과 컬렉션 fetch join을 함께 쓰지 않습니다(메모리 페이징 경고).
  컬렉션이 필요한 페이징 조회는 `default_batch_fetch_size: 100`으로 해결합니다.
- 쿼리 수는 `logging.level.org.hibernate.SQL=debug` 또는 p6spy로 확인하는 습관을 들입니다. N+1 해결 커밋에는 전후 쿼리 수를 메시지에 적습니다.
- 카운트 쿼리가 무거워지면 `Slice`를 씁니다.
- 복잡한 동적 조회는 QueryDSL, 화면에 필요한 컬럼만 필요하면 DTO Projection으로 바로 조회합니다.

---

## 8. 동시성

- 여러 요청이 동시에 바꿀 수 있는 데이터(대출 가능 권수, 예약 순번, 쿠폰 사용, 결제)는 어떤 방식으로 막을지 정하고 구현합니다.
  - 충돌이 드문 경우: 낙관적 락(`@Version`)
  - 충돌이 잦거나 반드시 막아야 하는 경우: 비관적 락(`@Lock(PESSIMISTIC_WRITE)`)
  - 중복 생성 방지: UNIQUE 제약 + `DataIntegrityViolationException` 처리
- 동시성을 처리한 기능에는 동시 요청 테스트(`ExecutorService` + `CountDownLatch`)를 붙입니다.

---

## 9. API와 화면 URL (처음부터)

- 화면: `/books`, `/loans/my` 처럼 명사 복수형. 세션 인증.
- API: `/api/v1/...`. JWT 인증. 응답은 공통 `ApiResponse<T>` 포맷, 목록은 `PageResponse<T>`.
- 관리자 기능은 admin 앱(9xxx 포트)에만 둡니다. 앱이 나뉘어 있어도 URL은 `/admin/**`, `/api/v1/admin/**`로 시작합니다. 나중에 리버스 프록시나 로그에서 구분하기 쉽게 하기 위해서입니다.
- HTTP 상태 코드는 의미에 맞게 씁니다. (생성 201, 검증 실패 400, 인증 실패 401, 권한 없음 403, 없음 404, 상태 충돌 409)

---

## 10. Security (처음부터)

- 비밀번호는 `BCryptPasswordEncoder`. 평문 비밀번호는 로그에도 남기지 않습니다.
- 현재 사용자는 Controller에서 `@AuthenticationPrincipal`로 받고, Service에는 `memberId`(Long)만 넘깁니다. Service가 Security 타입을 알지 않게 합니다.
- 본인 자원 확인(내 주문, 내 대출)은 Service에서 `memberId`를 비교합니다. URL의 ID만 믿지 않습니다.
- JWT 비밀키, DB 비밀번호는 환경변수로 주입합니다. `application-local.yml`은 `.gitignore`에 넣고 `application-local.yml.example`을 커밋합니다. (user, admin 앱마다 하나씩)
- 비밀번호, 계정 정보는 개발용이라도 저장소(코드, 예시 파일, 문서)에 남기지 않습니다. 예시 파일에는 키 이름만 두고 값은 비워 둡니다.
- 로컬 Oracle 비밀번호는 루트 `.env`(git 제외)에 두고 `docker-compose.yml`이 읽습니다. 형식은 `.env.example`을 봅니다.

---

## 11. Validation

- 요청 DTO에는 형식 검증만 (`@NotBlank`, `@Size`, `@Email`, `@Positive`).
- "서재에 이미 있음", "대출 한도 초과" 같은 상태 검증은 Service에서 합니다.
- 커스텀 검증이 필요하면 `@Constraint` 어노테이션을 하나 만들어 봅니다(예: ISBN-13 체크섬).

---

## 12. 테스트 (처음부터)

| 대상 | 방식 |
|---|---|
| Domain (Entity) | core 모듈. 순수 JUnit. 스프링 컨텍스트 없이 규칙 검증 |
| Service | Mockito (`@ExtendWith(MockitoExtension.class)`) |
| Repository | core 모듈. `@DataJpaTest` + Testcontainers(Oracle). 쿼리가 있는 메서드만 |
| Controller | `@WebMvcTest` + `@MockitoBean`. 바인딩, 검증, 응답 포맷만 |
| 통합 | 핵심 시나리오(대출-반납, 주문-결제-환불) 각 1개, `@SpringBootTest` + Testcontainers |

- 메서드명은 `메서드명_조건_기대결과`, 한글 허용: `loan_최대권수초과_예외()`. `@DisplayName`으로 문장 설명.
- `// given`, `// when`, `// then` 주석 세 줄을 넣습니다. 테스트 하나에 검증 대상은 하나.
- 핵심 규칙(대출 한도, 연체 정지, 연장 불가, 중복 결제, 환불 조건)은 반드시 실패 케이스 테스트를 포함합니다.
- 테스트 객체 생성은 `MemberFixture.activeMember()` 같은 Fixture 클래스로 모읍니다. 여러 모듈에서 쓰는 Fixture와 `TestcontainersConfiguration`은 core의 `src/testFixtures`에 둡니다.
- 시간이 관여하는 로직은 `Clock`을 주입받아 테스트에서 고정합니다.
- 기능을 커밋할 때 그 기능의 테스트도 같이 커밋합니다.

---

## 13. 로깅

- `@Slf4j`. 문자열 연결 대신 플레이스홀더: `log.info("loan created. loanId={}, memberId={}", loanId, memberId)`.
- 비즈니스 이벤트(대출, 반납, 결제 승인, 환불)는 INFO, 예상된 비즈니스 예외는 WARN, 예상하지 못한 예외는 ERROR.
- 개인정보(이메일, 전화번호, 비밀번호)와 토큰은 로그에 남기지 않습니다.

---

## 14. Thymeleaf

- 레이아웃은 Layout Dialect로 `templates/layout/default.html` 하나. 페이지는 content 영역만 작성합니다.
- 공통 조각은 `templates/fragments/` 아래(헤더, 페이징, 알림 메시지).
- 폼은 `th:object` + `th:field`, 검증 오류는 `th:errors`. 자바스크립트로 폼 검증을 중복하지 않습니다.
- 화면 컨트롤러 메서드명은 GET은 `xxxForm`, POST는 `xxx`. 뷰 이름은 URL 구조와 동일하게 (`book/detail`, `admin/book/form`).
- 처리 후에는 redirect + `RedirectAttributes.addFlashAttribute("message", ...)`로 결과를 안내합니다(PRG 패턴).

---

## 15. 네이밍과 스타일 (처음부터)

- 패키지는 소문자 단수(`member`, `loan`, `order`).
- 클래스는 역할 접미사: `LoanService`, `LoanRepository`, `LoanApiController`, `LoanViewController`, `LoanResponse`, `LoanCreateRequest`.
- Boolean 메서드는 `is/has/can`. 조회는 `find`(없을 수 있음, Optional) / `get`(반드시 있음, 없으면 예외)으로 구분합니다.
- 매직 넘버 금지. 정책 값은 `@ConfigurationProperties` 클래스(`LoanPolicy`) 또는 상수.
- 날짜는 `LocalDate`, `LocalDateTime`. `Clock`을 주입받아 `LocalDate.now(clock)`으로 씁니다.
- Lombok은 `@Getter`, `@RequiredArgsConstructor`, `@Slf4j`, `@Builder`, `@NoArgsConstructor`만 사용합니다.
- 의존성 주입은 생성자 주입(`@RequiredArgsConstructor` + `private final`). 필드 주입 금지.
- 주석은 "왜"만 적습니다. "무엇"은 코드로 드러나야 합니다.
- 사용하지 않는 코드, 주석 처리한 코드는 커밋하지 않습니다.

---

## 16. Git (처음부터)

- 커밋 메시지 형식: `type: 요약` (한글). 본문에 "왜 이렇게 했는지" 한두 줄.
  - `feat:` 기능, `fix:` 버그, `refactor:` 동작 변화 없는 구조 변경, `perf:` 성능, `test:` 테스트, `docs:` 문서, `chore:` 설정과 빌드
- 브랜치는 기능 단위 `feat/loan-extend`, `fix/overdue-calc`. main에 PR로 머지합니다. 혼자여도 PR 설명을 씁니다.
- 개선 커밋은 before/after가 보이게 분리합니다: `perf: 내 대출 목록 N+1 해결 (쿼리 11회 → 2회)`.
- 커밋 하나는 빌드와 테스트가 통과하는 상태여야 합니다.

### PR 체크리스트
- [ ] 엔티티가 Controller 밖으로 나가지 않는다
- [ ] user와 admin이 서로 의존하지 않고, 두 앱에 같은 로직을 복사하지 않았다
- [ ] 쓰기 메서드에 `@Transactional`이 있다
- [ ] 이미 적용된 마이그레이션 파일을 고치지 않았고, 스키마 변경은 새 `V{n}__*.sql`로 추가했다
- [ ] 새 참조 컬럼에 인덱스가 있고, 저장 전에 참조 대상 존재를 확인한다
- [ ] 다른 애그리거트를 객체 참조하지 않았고, 객체 참조에는 `NO_CONSTRAINT`가 있다
- [ ] 새 비즈니스 규칙에 실패 케이스 테스트가 있다
- [ ] 예외를 삼키는 catch가 없다
- [ ] 로그에 개인정보가 없다
- [ ] 코드, 예시 파일, 문서에 비밀번호나 계정 정보가 없다
- [ ] 목록 조회에 페이징이 있다

---

## 17. 코드 품질 도구

- Spotless(Google Java Format) Gradle 플러그인. `./gradlew spotlessApply`로 포맷, CI에서 `spotlessCheck`.
- GitHub Actions: push와 PR마다 `./gradlew build` (테스트 포함).
- JaCoCo로 커버리지를 측정만 하고 숫자 목표는 두지 않습니다. 핵심 Service의 커버리지만 신경 씁니다.

---

## 18. 학습 내용 반영 (직접 작성)

> 기존에 배운 내용 중 이 프로젝트에 꼭 넣을 기능이나 기법을 적습니다.
> 항목마다 "어느 서비스의 어떤 기능에 넣을지"도 함께 적어 두면 설계할 때 바로 쓸 수 있습니다.

| 학습 내용 | 적용할 서비스 / 기능 | 상태 |
|---|---|---|
|  |  |  |
