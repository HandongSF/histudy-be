# 로깅 운영 계약

## 출력 대상

- 콘솔 로그는 컨테이너 표준 출력으로 기록합니다.
- 파일 로그는 `${LOG_PATH}/histudy-be.log`에 기록합니다.
- Docker Compose 기본값에서 `${LOG_PATH}`는 `/webapp/log`이며, `webapp_logs` 볼륨으로 영속화됩니다.
- 날짜와 크기를 함께 기준으로 압축 롤링하며, 파일명은 `histudy-be.YYYY-MM-DD.INDEX.log.gz` 형식입니다.

## 기본 보존 정책

| 설정 | 기본값 | 의미 |
| --- | --- | --- |
| `ROOT_LOG_LEVEL` | `INFO` | root logger의 최소 로그 레벨 |
| `LOGBACK_ROLLINGPOLICY_MAX_HISTORY` | `30` | 보존할 일수 |
| `LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE` | `100MB` | 개별 로그 파일의 최대 크기 |
| `LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP` | `2GB` | 롤링 로그 전체 용량 상한 |
| `WEBHOOK_DISCORD_TIMEOUT_MS` | `3000` | Discord webhook 연결·응답 대기 시간(밀리초) |

환경별 값은 Docker Compose 환경 변수로 변경할 수 있습니다. `TOTAL_SIZE_CAP`은 `MAX_FILE_SIZE`보다
작지 않아야 합니다. 로컬 실행에서 `LOG_PATH`를 지정하지 않으면 `/tmp/histudy-be`를 사용합니다.

## 요청 상관관계 ID

요청 처리 계층이 MDC의 `request_id`를 설정하면 콘솔·파일 로그의 각 줄에 같은 값이 출력됩니다.
HTTP 요청/응답 헤더와 예외 응답의 `requestId`에도 같은 식별자를 사용하며, 요청 로깅과 예외 응답
계약은 각 기능 PR에서 함께 완성합니다.

## 필드 정책

애플리케이션 로그는 이벤트 이름을 첫 토큰으로 두고, 나머지는 `snake_case` 키-값 필드로 기록합니다.
필드 값은 운영 검색과 상관관계 추적에 필요한 최소 정보만 허용합니다.

| 이벤트 | 필수 필드 | 레벨 | 용도 |
| --- | --- | --- | --- |
| `http_request` | `request_id`, `method`, `route`, `status`, `duration_ms`, `role` | `INFO` | 요청별 처리 결과와 4xx/5xx 집계 |
| `authentication_failed` | `request_id`, `exception_type` | `WARN` | 인증 실패 보안 이벤트 |
| `authorization_denied` | `request_id`, `exception_type` | `WARN` | 권한 거부 보안 이벤트 |
| `unhandled_exception` | `request_id`, `error_id`, `exception_type` | `ERROR` | 컨트롤러 예외 진단과 내부 stack trace 연결 |
| `discord_notification_failed` | `request_id`, `error_id`, `exception_type` | `ERROR` | 장애 알림 전송 실패 |
| `discord_notification_timeout` | `request_id`, `error_id` | `WARN` | 장애 알림 webhook 시간 초과 |

`role`은 검증된 JWT 역할(`USER`, `MEMBER`, `ADMIN`)만 기록하며, 인증 정보가 없으면 `anonymous`,
역할 값이 없거나 허용 목록 밖이면 `unknown`으로 기록합니다. `route`는 Spring의 경로 템플릿을
사용하고, 템플릿을 확인할 수 없으면 `UNKNOWN`으로 기록하여 원시 경로의 식별자를 피합니다.
4xx는 `http_request`의 `status`로 집계하고 예외 stack trace를 남기지 않습니다.

컨트롤러에서 처리되지 않은 예외의 `error_id`는 해당 응답·내부 로그·장애 알림을 연결하는 UUID입니다.
클라이언트 응답에는 `requestId`와 `errorId`를 사용하고, 4xx 응답에는 `errorId`를 포함하지 않습니다.

향후 도메인 감사 이벤트는 다음 필드만 확장해 사용합니다.

- 공통: `event`, `request_id`, `role`, `result`, 필요한 경우 `reason_code`
- 행위자: `actor_id`는 서버 내부 식별자만 사용하며 이메일·학번·외부 subject는 기록하지 않음
- 대상: `target_type`, `target_id` 및 필요한 도메인 ID(`academic_term_id`, `course_id`, `group_id`, `report_id`, `banner_id`)
- 수치: `count`, `duration_ms`, `no_op` 등 집계 가능한 값

원칙적으로 JWT, 본문, 쿼리 원문, 쿠키, 이메일, 학번, 리포트 본문, 원본 CSV, 파일명·URL,
예외 메시지 원문은 공통 구조화 필드와 외부 장애 알림에 기록하거나 전송하지 않습니다. 5xx
stack trace는 서버 내부 로그에만 남기고 Discord 알림에는 `error_id`, `request_id`, 예외 유형과
HTTP 메서드만 전송합니다.

## 매칭 실행 감사 로그

`matching_executed`는 관리자 매칭의 완료 결과를 INFO로 한 번 기록합니다. DB 트랜잭션이
커밋된 이후에만 출력하며 롤백, 저장 실패, 현재 학기 부재에는 출력하지 않습니다.
실패는 기존 HTTP·예외 로그로 추적합니다. 트랜잭션 밖에서 발행한 이벤트도 출력하지 않습니다.

| 필드 | 의미 |
| --- | --- |
| `request_id`, `actor_id`, `role` | 요청 ID, 관리자 내부 사용자 ID, 검증된 JWT 역할 |
| `academic_term_id` | 매칭 대상 학기 ID |
| `result` | 그룹 생성 시 `success`, 변경 없으면 `no_op` |
| `reason_code` | no_op에만 포함: 신청자 없음 `NO_UNASSIGNED_APPLICANTS`, 그룹 구성 불가 `NO_ELIGIBLE_GROUPS` |
| `applicant_count` | 실행 시작 시 미배정 신청자 수 |
| `assigned_count`, `remaining_count` | 이번 실행의 배정·미배정 인원; 합계는 applicant_count |
| `created_group_count` | 이번 실행에서 생성한 그룹 수, 기존 그룹 제외 |
| `duration_ms` | 서비스 진입부터 이벤트 생성까지; 커밋·로그 출력 시간 제외 |

일부 신청자가 남아도 그룹이 생성되면 success입니다. 행위자는 권한 검사 이후 JWT subject의
이메일로 조회하되 이메일은 이벤트나 로그에 보관하지 않습니다. 사용자 또는 subject가 없으면
actor_id=unknown으로 기존 매칭 동작을 유지하며 DB 조회 장애는 숨기지 않습니다. 요청 ID가
없으면 unknown으로 기록합니다. 이벤트에는 ID와 집계값만 복사하며 커밋 이후 엔티티나 MDC를
다시 읽지 않습니다. 신청자 목록·친구 관계·과목 목록·개인정보는 포함하지 않습니다.

출력은 기존 콘솔·파일 로그를 사용합니다. 커밋 이후 출력이므로 DB와 감사 로그의 원자적
영구 보존은 보장하지 않습니다.

## 학기·과목 감사 이벤트

학기 생성과 전환, 과목 교체와 삭제는 관리자 권한 검사 뒤 확보한 `AuditContext`를 전달한다. 완료 이벤트는 DB 커밋 이후 INFO로 한 번 기록하며 외부 트랜잭션 롤백·flush 실패·트랜잭션 없는 이벤트 발행에서는 출력하지 않는다. `request_id`, `actor_id`, `role`은 발행 시점 값이며 행위자 부재는 `unknown`이다. CSV 원문·파일명·과목명·교수명·사용자 개인정보·예외 원문은 포함하지 않는다.

| 이벤트 | 추가 필드 | 결과 |
| --- | --- | --- |
| `academic_term_created` | `academic_term_id` | `success` |
| `current_term_changed` | `previous_term_id`, `academic_term_id` | `success`, 이미 현재 학기면 `no_op` / `ALREADY_CURRENT` |
| `courses_replaced` | `academic_term_id`, `previous_count`, `replacement_count` | `success`, 빈 입력이면 `no_op` / `EMPTY_INPUT` |
| `course_deleted` | `academic_term_id`, `course_id`, `legacy` | `success`, 구형 경로의 대상 부재는 `no_op` / `COURSE_NOT_FOUND` |

`previous_count`는 선택된 현재 학기에 한정한 DB count 쿼리로 얻는 교체 전 과목 수(long), `replacement_count`는 저장 요청 과목 수다. 빈 입력은 기존처럼 학기 조회 없이 종료하므로 학기와 이전 건수는 `unknown`이다. 이전 현재 학기가 없거나 구형 삭제의 대상 학기를 알 수 없으면 해당 ID는 `unknown`이다. 구형 삭제는 기존 1/0 반환과 직접 삭제 정책을 유지하되 완료 기록을 위해 서비스 트랜잭션을 사용한다.

사용 중 과목의 교체·신형 삭제 거절은 같은 이벤트 이름에 `result=rejected reason_code=COURSE_IN_USE`를 넣어 동기 INFO로 기록한다. 거절 이벤트는 커밋에 의존하지 않아 예외로 롤백되어도 남으며 완료 이벤트는 발행하지 않는다. 다른 실패는 기존 HTTP·예외 로그로 추적한다. 이 로그는 DB와 원자적인 영구 보존을 보장하지 않는다.
