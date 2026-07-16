# 아키텍처

## 코드 구조

- 애플리케이션 코드: `src/main/java/edu/handong/csee/histudy`
- 테스트 코드: `src/test/java/edu/handong/csee/histudy`
- 설정 파일: `src/main/resources`
- HTTP 계약: `api-docs.yaml`

주요 패키지는 다음과 같습니다.

- `controller`: HTTP 엔드포인트와 응답 처리
- `controller/form`, `dto`: 요청/응답 경계 타입
- `service`: 비즈니스 로직과 오케스트레이션
- `repository`, `repository/jpa`, `repository/impl`: 영속성과 조회 로직
- `domain`: JPA 엔티티와 엔티티에 가까운 도메인 동작
- `matching/application`: 매칭 유스케이스의 트랜잭션과 저장소 조율
- `matching/domain`: 저장소나 Spring에 의존하지 않는 친구·과목 우선 매칭 규칙
- `config`, `interceptor`, `jwt`: 인증과 요청 파이프라인 구성
- `exception`, `util`: 공통 예외와 유틸리티

## 요청 흐름

기본 요청 흐름은 다음과 같습니다.

`controller -> service -> repository -> domain`

유지해야 할 역할 분리는 다음과 같습니다.

- 컨트롤러는 HTTP 요청 바인딩, 응답, 권한 검사를 담당합니다.
- 서비스는 비즈니스 로직과 여러 리포지토리 조합을 담당합니다.
- 리포지토리는 비즈니스 정책이 아니라 영속성/조회 형태를 담당합니다.
- DTO/form 타입은 API 경계를 정의하며, JPA 엔티티를 직접 노출하지 않습니다.

## 점진적 모듈화 원칙

현재의 전역 레이어 구조는 한 번에 재배치하지 않습니다. 기능을 변경하는 시점에 유스케이스 단위로
`api/controller -> application/service -> domain <- repository/infrastructure` 의존 방향을 적용하고,
기존 진입점은 새 구현으로 위임하는 파사드로 유지한 뒤 호출자가 모두 전환되면 제거합니다.

레이어별 의존 규칙은 다음과 같습니다.

- 도메인은 `controller`, HTTP form, API DTO, 서비스, 리포지토리에 의존하지 않습니다.
- 컨트롤러는 요청 바인딩, 응답 변환, 권한 검사만 담당하고 리포지토리를 직접 호출하지 않습니다.
- 애플리케이션 서비스는 트랜잭션과 유스케이스를 조율하며 API 응답 모델을 도메인에 전달하지 않습니다.
- 리포지토리와 외부 연동 구현은 도메인 및 애플리케이션 계층이 정의한 계약을 따릅니다.
- JPA 엔티티 매핑은 점진 전환 중에도 유지하며, 패키지 이동과 DB 스키마 변경을 한 작업에서 함께 하지 않습니다.

모듈화는 매칭처럼 규칙이 복잡한 핵심 기능부터 시작합니다. 배너, 알림, 파일 저장 같은 지원 기능은
기능별 패키지로 모으되 단순 CRUD마다 command, port, adapter를 만들지 않습니다. 각 전환 작업은 HTTP
계약과 DB 매핑을 보존하고, 관련 유저 스토리 테스트와 패키지 의존 테스트를 통과해야 합니다.

## 인증 모델

이 프로젝트는 Spring Security filter chain을 사용하지 않습니다.
인증은 다음 구성으로 처리됩니다.

- `WebConfig`: `AuthenticationInterceptor` 등록
- `AuthenticationInterceptor`: `JwtService`로 Bearer 토큰을 검증하고 요청에 `Claims` 저장
- 컨트롤러: 엔드포인트별 `Role.isAuthorized(...)` 검사 수행

인증 포함/제외 경로는 `application.yml`의 다음 설정으로 제어됩니다.

- `custom.path-patterns.include`
- `custom.path-patterns.exclude`

즉, 인증 변경은 보통 설정과 컨트롤러 동작을 함께 수정해야 합니다.

## API 표면

컨트롤러는 다음 엔드포인트 계열로 나뉩니다.

- `PublicController`: `/api/public/**`
- `AuthController`: `/api/auth/**`
- `UserController`: `/api/users`, `/api/v2/users`, `/api/users/me`, `/api/v2/users/me/forms`
- `ApplyFormController`: `/api/forms`, `/api/v2/forms`
- `CourseController`: `/api/courses`
- `TeamController`: `/api/team/**`
- `AdminController`: `/api/admin/**`
- `BannerAdminController`: `/api/admin/banners/**`

엔드포인트 동작이 바뀌면 해당 컨트롤러 테스트와 `api-docs.yaml`을 함께 갱신해야 합니다.

## 이미지와 정적 리소스

- 리소스 URL은 `WebConfig.addResourceHandlers`로 연결합니다.
- 저장 위치와 공개 경로는 다음 설정으로 정의합니다.
  - `custom.resource.location`
  - `custom.resource.path-pattern`
  - `custom.resource.path`
- `ImageService`는 리포트 이미지를 담당합니다.
- `BannerService`는 배너 이미지와 순서를 담당합니다.
- `ImagePathMapper`는 저장 경로와 공개용 전체 경로를 변환하는 기준 유틸리티입니다.

## 테스트 구조

- 컨트롤러 테스트: 엔드포인트 동작과 응답 검증
- 서비스 테스트: 비즈니스 규칙과 매칭 로직
- 리포지토리 테스트: 커스텀 조회 동작
- `service/repository/fake`: 서비스 테스트용 인메모리 테스트 더블
- `support`: 공통 테스트 헬퍼

`perf` 태그가 붙은 테스트는 매칭 성능 검증용이며 기본 `test` 작업에서는 제외됩니다.
