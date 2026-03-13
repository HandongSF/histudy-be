# 워크플로

## 문서 목적

- 이 문서는 반복해서 실행하는 명령, 설정 경로, 배포 절차처럼 재현성을 높이는 정보만 담는다.
- 도메인 규칙은 `docs/domain.md`, 구조와 요청 흐름은 `docs/architecture.md`, 기능별 검증 기준은 `docs/acceptance-criteria.md`를 기준으로 본다.

## 핵심 명령어

- 앱 실행: `./gradlew bootRun`
- 기본 테스트 실행: `./gradlew clean test`
- 커버리지 리포트 생성: `./gradlew clean test jacocoTestReport`
- 성능 테스트 실행: `./gradlew perfTest`
- 전체 빌드 실행: `./gradlew build`
- 단일 테스트 클래스 실행: `./gradlew test --tests "*TeamServiceMatchingAlgorithmTest"`

## 로컬 실행 메모

- `src/main/resources/application.yml`은 공용 기본값으로 유지합니다.
- `application-local.yml`은 로컬 프로필이 실제로 포함된 경우에만 사용합니다. 현재 저장소는 추가 설정 없이는 이를 자동 로드하지 않습니다.
- JWT 시크릿과 웹훅 값은 `application.yml`의 개발 기본값과 환경 변수(`JWT_SECRET`, `WEBHOOK_DISCORD`)를 함께 사용합니다.
- 기본 로컬 데이터소스는 인메모리 H2입니다.

## 배포 설정

- `docker-compose.yml`은 `SPRING_CONFIG_ADDITIONAL_LOCATION=file:/config/application-${ACTIVE_PROFILE:-dev}.yml` 방식으로 외부 Spring 설정을 읽습니다.
- 컨테이너를 시작하기 전에 `${CONFIG_PATH}` 아래에 맞는 설정 파일을 준비해야 합니다.
- GitHub Actions 배포는 `.github/workflows/ci.yml`, `.github/workflows/deploy-dev.yml`, `.github/workflows/deploy-prod.yml`로 구성합니다.
- `ci.yml`은 `pull_request`와 `main` push에서 테스트를 수행합니다.
- `deploy-dev.yml`은 `workflow_run`으로 CI 성공 이후에만 실행되며, 내부 `publish -> deploy` 순서로 Docker 이미지 빌드, GHCR 푸시, digest 고정 `IMAGE_REF` 배포를 수행합니다.
- `main`에서 과거 커밋의 CI를 다시 실행해도, 해당 SHA 기준으로 이미지를 다시 빌드하고 배포할 수 있습니다.
- 게시 이미지는 `sha-<커밋 7자리>` 태그만 사용하며, 같은 워크플로 안에서 계산한 digest 포함 `IMAGE_REF`를 dev 배포에 바로 사용합니다.
- `deploy-prod.yml`은 `v*` 태그 push 또는 `workflow_dispatch` 입력 `tag`로 실행되며, prod runner에는 선택된 태그를 `DEPLOY_TAG` 환경 변수로 넘기고 저장소 변수 `DEPLOY_PROD_COMMAND` 한 줄을 실행합니다.
- self-hosted 배포에 필요한 저장소 변수는 다음과 같습니다.
  - `DEPLOY_CONFIG_PATH`: 호스트의 Spring 설정 디렉터리 경로
  - `DEPLOY_ACTIVE_PROFILE`: 배포 프로필, 기본값 `dev`
  - `DEPLOY_HOST_PORT`: 공개 포트, 기본값 `8080`
  - `DEPLOY_PROD_COMMAND`: prod runner에서 실행할 배포 스크립트 한 줄

## 문서 갱신 규칙

- 반복 명령, 설정 경로, 배포 절차가 바뀌면 `docs/workflows.md`를 갱신한다.
- 도메인 규칙이나 인수 기준이 바뀌면 `docs/domain.md`, `docs/acceptance-criteria.md`를 갱신한다.
- 구조, 인증 흐름, API 경계가 바뀌면 `docs/architecture.md`와 `api-docs.yaml`을 함께 확인한다.
- 탐색 문서 집합이나 문서 역할이 바뀌는 경우에만 `AGENTS.md`를 갱신한다.
