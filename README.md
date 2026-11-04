# book-platform

도서를 주제로 한 Spring Boot 학습용 서비스 2개를 각각 독립된 컨테이너로 구성하는 워크스페이스입니다.
Spring 기본기(계층 구조, JPA 연관관계, 트랜잭션, 예외 처리, Security, 테스트)를 정확하게 쓰는 것을 목표로 하며,
기능 단위 커밋을 남겨 포트폴리오로 활용합니다.

| 서비스 | 폴더 | 한 줄 설명 | 회원 포트 | 관리자 포트 |
|---|---|---|---|---|
| 도서 대출 관리 | `library-loan-service/` | 회원이 종이책을 대출, 반납, 예약하는 도서관 시스템 | 8081 | 9081 |
| ebook 커머스 | `ebook-commerce-service/` | 회원이 전자책을 장바구니에 담아 결제하고 내 서재에서 읽는 쇼핑몰 | 8082 | 9082 |

각 서비스는 Gradle 멀티모듈(`core` 공통 도메인, `user` 회원 앱, `admin` 관리자 앱)이고, 회원 앱과 관리자 앱을 다른 포트로 따로 띄웁니다.

두 서비스는 서로 호출하지 않는 독립 애플리케이션이며, 공통 문서와 인프라와 함께 저장소 하나(모노레포)에서 관리합니다.
공통 인프라(Oracle)는 루트의 `docker-compose.yml` 하나로 띄우고, 서비스마다 별도 스키마(사용자)를 씁니다.

## 문서

- `docs/00-architecture.md` : 컨테이너 구성, 기술 스택, 패키지 구조, 인증 흐름
- `docs/01-rules.md` : 두 서비스 공통 개발 규칙 (DB 설계, 엔티티, 계층, 예외, 트랜잭션, 테스트, Git)
- `library-loan-service/docs/` : 도서 대출 서비스 ERD, 화면 흐름
- `ebook-commerce-service/docs/` : ebook 커머스 ERD, 화면 흐름
- 각 서비스의 `*-core/src/main/resources/db/migration/` : Flyway 마이그레이션 (스키마 기준)

## 설계 결정

### PK에 UUID 대신 시퀀스를 쓴 이유

- Oracle에는 UUID 타입이 없어 `RAW(16)`이나 `VARCHAR2(36)`으로 저장해야 합니다. 조회할 때 읽기 어렵거나 저장 공간이 커집니다.
- 랜덤 UUID는 B-tree 인덱스의 아무 위치에나 들어가 인덱스 분할과 단편화가 생깁니다.
- FK를 쓰지 않는 대신 참조 컬럼마다 인덱스를 만듭니다. 키가 커지면 이 인덱스들도 모두 커집니다.
- JPA에서 ID를 미리 만들어 넣으면 `save()`가 `merge()`로 동작해 SELECT가 한 번 더 나갑니다.
- ID를 추측해 남의 데이터에 접근하는 문제는 서비스에서 로그인 회원과 데이터 소유자를 비교해 막습니다.
  외부에 숫자 ID를 숨겨야 하는 테이블이 생기면 PK는 그대로 두고 노출용 UUID 컬럼만 추가합니다.

## 진행 단계

1. 테이블 설계와 화면 흐름 확정 (FK 없는 설계, Flyway `V1__init.sql`)
2. 프로젝트 뼈대 생성 (Gradle 멀티모듈 core/user/admin, 공통 설정, 예외 처리, Testcontainers)
3. (다음) 도서 대출 서비스 구현 (도메인별로 엔티티 → 서비스 → API → 화면)
4. ebook 커머스 서비스 구현
5. Docker, GitHub Actions 연결
6. 확장: 서비스 간 통신 (대출 이력 기반 ebook 추천). library와 ebook이 API 또는 메시지로 통신하는 MSA 요소를 하나 추가
