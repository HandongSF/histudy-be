# 학기 서비스 API 경계 분리 설계

## 목적

PR #208과 #209에서 도메인 및 컨트롤러의 계층 의존 방향을 정리한 흐름을 이어,
`AcademicTermService`가 HTTP 요청 타입인 `AcademicTermForm`에 의존하지 않도록 한다.
이번 변경은 기능별 패키지 이동 전에 서비스 계층과 API 계층 사이의 입력 경계를 작게
검증하는 첫 사례다.

## 현재 상태

`AdminController`는 `AcademicTermForm`으로 HTTP 요청을 바인딩한 뒤 해당 객체를 그대로
`AcademicTermService.createAcademicTerm`에 전달한다. 이 때문에 서비스 계층이
`controller.form` 패키지를 직접 참조한다.

서비스 계층에는 다음과 같은 `controller.form` 의존이 남아 있다.

- `AcademicTermService` → `AcademicTermForm`
- `ReportService` → `ReportForm`
- `UserService` → `ApplyForm`, `UserForm`
- `BannerService` → `BannerForm`, `BannerReorderForm`

## 변경 범위

### 학기 생성 입력 분리

`AcademicTermService.createAcademicTerm`의 입력을 다음과 같이 변경한다.

```java
public void createAcademicTerm(Integer year, TermType semester)
```

`AdminController`는 기존과 동일하게 `AcademicTermForm`으로 HTTP 요청을 받고,
`form.getYear()`와 `form.getSemester()`를 서비스 메서드에 전달한다.

`year`는 기존 form과 동일하게 `Integer`를 사용해 null 입력에서 불필요한 언박싱 동작
변경을 만들지 않는다. 별도의 Command 타입은 추가하지 않는다. 서비스 입력 표현은 다음
기준을 일관되게 적용한다.

- HTTP 요청 바인딩 form은 컨트롤러 경계 밖으로 전달하지 않는다.
- 필수 입력이 한두 개이고 각 값의 의미가 메서드 이름과 타입으로 명확하면 개별 인자를
  사용한다.
- 입력이 세 개 이상이거나 값들이 함께 전달되어야 하나의 유스케이스 의미를 이루면
  Command를 사용한다.
- 선택 필드 조합에 따라 수정 의미가 달라지거나 입력 자체에 애플리케이션 검증 규칙이
  있으면 Command를 사용한다.
- 여러 진입점이 동일한 서비스 입력 계약을 공유하면 Command를 사용한다.
- Command 도입이 Port, Adapter, Mapper 생성을 자동으로 요구하지는 않는다.

따라서 값 두 개만 전달하고 별도의 조합 규칙이 없는 학기 생성은 개별 인자를 사용한다.
이 기준은 “필요할 때”라는 주관적 판단을 줄이면서 단순 CRUD에 입력 타입이 불필요하게
증가하는 것도 방지한다.

### 서비스 계층 의존성 기준선

`LayerDependencyTest`에 서비스 소스가 `controller` 패키지에 의존하는 위치를 수집하는
규칙을 추가한다. 기존 레거시 의존은 아래 다섯 파일·타입 조합만 명시적으로 허용한다.

- `BannerService` → `BannerForm`
- `BannerService` → `BannerReorderForm`
- `ReportService` → `ReportForm`
- `UserService` → `ApplyForm`
- `UserService` → `UserForm`

`AcademicTermService` 의존은 허용 목록에 포함하지 않는다. 이후 새로운 서비스→controller
의존이 생기거나 기존 의존이 제거되면 테스트가 실패해 기준선 갱신과 변경 의도 검토를
요구하도록 한다.

이 기준선은 영구적인 허용 규칙이 아니라 남은 부채를 감소 방향으로만 움직이게 하는
래칫이다. 후속 PR에서 각 의존을 제거할 때 해당 허용 항목도 함께 삭제한다.

## 동작 및 계약

- `/api/admin/academicTerm`의 요청 JSON과 응답 상태는 변경하지 않는다.
- `AcademicTermForm`의 필드와 검증 방식은 변경하지 않는다.
- 연도나 학기가 누락되면 기존 API 계약에 따라 `400 Bad Request`로 처리한다.
- 중복 학기 검증과 저장되는 `AcademicTerm` 값은 기존과 동일하다.
- DB 스키마와 JPA 매핑은 변경하지 않는다.
- 기존 `AcademicTermDto` 응답 구조는 이번 PR 범위에 포함하지 않는다.

## 테스트

### 서비스 테스트

`AcademicTermServiceTest`는 `AcademicTermForm` 대신 연도와 학기 값을 직접 전달한다.
중복 학기 거절과 신규 학기 저장 동작이 기존과 동일함을 검증한다.

### 컨트롤러 테스트

`AdminControllerTest`는 요청 form의 연도와 학기가 서비스 메서드의 개별 인자로
전달되는지 검증한다. HTTP 요청·응답 계약은 기존 테스트를 유지한다.

### 아키텍처 테스트

다음 두 조건을 검증한다.

1. 현재 남아 있는 서비스→controller 의존 집합이 명시된 레거시 기준선과 정확히 같다.
2. `AcademicTermService`에는 `controller.form` 참조가 없다.

소스 문자열 방식은 PR #209에서 보완한 주석 제외 판별을 재사용한다. ArchUnit 도입은
기능별 패키지 이동으로 규칙 수와 패키지 형태가 확장되는 시점에 다시 검토한다.

## 후속 순서

이번 PR 이후에는 입력 크기와 핵심 도메인 우선순위를 고려해 다음 순서로 진행한다.

1. `ReportService`에서 `ReportForm` 제거
2. `UserService`에서 `ApplyForm`, `UserForm` 제거
3. `BannerService`에서 배너 form 제거
4. 서비스→controller 허용 목록이 비면 전역 금지 규칙으로 단순화
5. 매칭 유스케이스를 기능별 패키지로 이동하고 제한적 전술 DDD 적용

각 후속 PR은 앞선 PR에서 줄어든 기준선을 기반으로 하므로 순차 의존한다. 단, API 계약과
DB 매핑을 유지해 개별 PR은 독립적으로 검토하고 되돌릴 수 있어야 한다.
