# AGENTS

이 저장소는 에이전트가 빠르게 이해할 수 있도록 구성되어 있습니다. `AGENTS.md`는 탐색 레이어로 사용하고, `docs/principles.md`를 제품 의도의 기준 문서로 두며, `docs/`를 지속적인 저장소 맥락의 정식 위치로 취급합니다.

## 기준 파일

- `docs/principles.md`: 제품 의도, 핵심 원칙, 행위 경계, 비목표
- `README.md`: 사람 중심의 실행 방법, 로컬 설정, 배포 예시
- `api-docs.yaml`: 저장소에 버전 관리되는 HTTP 계약

## 문맥 지도

- `docs/domain.md`: 용어, 엔티티, 불변 조건, 매칭 규칙, 프라이버시 규칙, 리포트/이미지 의미론
- `docs/architecture.md`: 패키지 책임, 요청 흐름, 인증 모델, API 표면, 테스트 구조
- `docs/workflows.md`: 반복 작업 절차, 설정 경로, 운영 가드레일, 하니스 갱신 조건
- `docs/acceptance-criteria.md`: 기능별 성공 조건, 실패 조건, 테스트에서 확인해야 할 인수 기준

## 작업 규칙

- 제품 의도와 구현 가이드가 충돌하면 `docs/principles.md`를 우선합니다.
- `controller -> service -> repository` 분리를 유지합니다.
- 컨트롤러는 얇게 유지하고, 비즈니스 결정은 리포지토리로 내리지 않습니다.
- API 경계는 DTO/form으로 유지하고, 도메인 엔티티를 직접 노출하지 않습니다.
- 테스트를 추가하거나 수정할 때는 `docs/acceptance-criteria.md`를 먼저 확인합니다.
- 지속적 맥락이 바뀌면 해당 `docs/*.md`를 먼저 갱신하고, 탐색 구조가 바뀐 경우에만 `AGENTS.md`를 수정합니다.

## 빠른 명령어

- 앱 실행: `./gradlew bootRun`
- 테스트 실행: `./gradlew clean test`
- 성능 테스트 실행: `./gradlew perfTest`
- 커버리지 생성: `./gradlew clean test jacocoTestReport`
- 단일 테스트 클래스 실행: `./gradlew test --tests "*TeamServiceMatchingAlgorithmTest"`
