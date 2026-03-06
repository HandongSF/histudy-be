# 워크플로

## 핵심 명령어

- 앱 실행: `./gradlew bootRun`
- 기본 테스트 실행: `./gradlew clean test`
- 커버리지 리포트 생성: `./gradlew clean test jacocoTestReport`
- 성능 테스트 실행: `./gradlew perfTest`
- 전체 빌드 실행: `./gradlew build`
- 단일 테스트 클래스 실행: `./gradlew test --tests "*TeamServiceMatchingAlgorithmTest"`

## 로컬 설정

- `src/main/resources/application.yml`은 공용 기본값으로 유지합니다.
- `application-local.yml`은 로컬 프로필이 실제로 포함된 경우에만 사용합니다. 현재 저장소는 추가 설정 없이는 이를 자동 로드하지 않습니다.
- JWT 시크릿과 웹훅 값은 `application.yml`의 개발 기본값과 환경 변수(`JWT_SECRET`, `WEBHOOK_DISCORD`)를 함께 사용합니다.
- 기본 로컬 데이터소스는 인메모리 H2입니다.

## 배포 설정

- `docker-compose.yml`은 `SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/application-${ACTIVE_PROFILE:-dev}.yml` 방식으로 외부 Spring 설정을 읽습니다.
- 컨테이너를 시작하기 전에 `${CONFIG_PATH}` 아래에 맞는 설정 파일을 준비해야 합니다.
- 루트의 `Jenkinsfile`은 빌드/배포 인프라 일부로 취급해야 합니다.

## 자주 발생하는 변경 경로

### API 엔드포인트 추가 또는 변경

- 컨트롤러와 해당 서비스를 수정한다.
- `src/test/java/.../controller` 아래 테스트를 추가하거나 갱신한다.
- `api-docs.yaml`을 갱신한다.
- 지속적인 API 의미나 노출 규칙이 바뀌면 `docs/domain.md`를 갱신한다.

### 매칭 또는 학기 로직 변경

- 필요한 서비스/리포지토리 코드를 수정한다.
- 서비스 테스트를 추가하거나 갱신한다.
- 관련 집중 테스트와 `./gradlew clean test`를 실행한다.
- 규칙이 바뀌면 `docs/domain.md`, `docs/acceptance-criteria.md`를 갱신한다.
- 제품 방향이나 핵심 원칙이 바뀐 경우에만 `docs/principles.md`를 갱신한다.

### 인증, 역할, 공개/보호 경계 변경

- 라우팅 가시성이 바뀌면 `application.yml`의 path pattern 설정을 수정한다.
- 역할 검사를 수행하는 컨트롤러를 갱신한다.
- 컨트롤러 테스트를 갱신한다.
- `api-docs.yaml`을 갱신한다.
- 지속적인 규칙이 바뀌면 `docs/domain.md`, `docs/architecture.md`, `docs/acceptance-criteria.md`를 갱신한다.

### 이미지 또는 배너 처리 변경

- 저장 경로 동작과 공개 URL 매핑을 함께 확인한다.
- 업로드, 재사용, 정렬 관련 테스트를 갱신한다.
- 저장 또는 매핑 모델이 바뀌면 `docs/architecture.md`를 갱신한다.

## 가드레일

- 외부 HTTP 동작이 바뀌면 `api-docs.yaml` 갱신을 생략하지 않는다.
- 프로필 연결이 없다면 `application-local.yml`이 자동 적용된다고 가정하지 않는다.
- 현재 학기 동작에 의존하기 전에, 현재 학기가 없을 때 어떻게 실패하는지 먼저 확인한다.

## 하니스 유지보수

지속적인 맥락이 바뀌면 다음 문서를 갱신한다.

- `docs/principles.md`: 제품 의도, 핵심 원칙, 경계 결정
- `docs/domain.md`: 불변 조건, 매칭 규칙, 프라이버시 규칙, 엔티티 의미론
- `docs/architecture.md`: 패키지 책임, 인증 모델, 리소스 파이프라인
- `docs/acceptance-criteria.md`: 기능별 성공/실패 기준과 테스트 관점 제약
- `docs/workflows.md`: 반복 절차와 운영 작업
- `AGENTS.md`: 기준 문서 집합이나 탐색 규칙이 바뀐 경우에만 수정
