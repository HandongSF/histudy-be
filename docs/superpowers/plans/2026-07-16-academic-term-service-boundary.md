# 학기 서비스 API 경계 분리 구현 계획

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `AcademicTermService`에서 HTTP form 의존을 제거하고 남은 서비스→controller 의존을 감소 전용 기준선으로 보호한다.

**Architecture:** `AdminController`가 `AcademicTermForm`을 HTTP 경계에서 해체해 `Integer year`, `TermType semester`를 서비스에 전달한다. `LayerDependencyTest`는 남아 있는 다섯 개의 레거시 import만 정확히 허용해 새 의존 추가와 기존 부채의 무의식적 유지를 모두 검토 대상으로 만든다.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, AssertJ, Mockito, Gradle

## Global Constraints

- HTTP 요청 바인딩 form은 컨트롤러 경계 밖으로 전달하지 않는다.
- 학기 생성에는 별도 Command를 만들지 않고 `Integer year`, `TermType semester`를 사용한다.
- `/api/admin/academicTerm`의 요청 JSON과 응답 상태를 변경하지 않는다.
- DB 스키마, JPA 매핑, 중복 학기 검증 및 현재 학기 동작을 변경하지 않는다.
- 기존 서비스→controller 의존 다섯 개만 레거시 기준선으로 허용한다.
- 테스트는 한글 메서드명, given-when-then 주석, AssertJ 규칙을 따른다.

---

### Task 1: 학기 서비스 입력 경계와 의존성 래칫

**Files:**
- Modify: `src/test/java/edu/handong/csee/histudy/architecture/LayerDependencyTest.java`
- Modify: `src/main/java/edu/handong/csee/histudy/service/AcademicTermService.java`
- Modify: `src/main/java/edu/handong/csee/histudy/controller/AdminController.java`
- Modify: `src/test/java/edu/handong/csee/histudy/service/AcademicTermServiceTest.java`
- Modify: `src/test/java/edu/handong/csee/histudy/controller/AdminControllerTest.java`

**Interfaces:**
- Consumes: `AcademicTermForm.getYear(): Integer`, `AcademicTermForm.getSemester(): TermType`
- Produces: `AcademicTermService.createAcademicTerm(Integer year, TermType semester): void`
- Preserves: `POST /api/admin/academicTerm` 요청 및 `201 Created` 응답

- [ ] **Step 1: 서비스→controller 의존 기준선 테스트 작성**

`LayerDependencyTest`에 다음 상수와 테스트를 추가한다.

```java
private static final Path SERVICE_SOURCE_DIRECTORY = MAIN_SOURCE_DIRECTORY.resolve("service");
private static final String CONTROLLER_PACKAGE_PREFIX =
    "edu.handong.csee.histudy.controller.";
private static final Set<String> LEGACY_SERVICE_CONTROLLER_DEPENDENCIES =
    Set.of(
        "BannerService.java: import edu.handong.csee.histudy.controller.form.BannerForm;",
        "BannerService.java: import edu.handong.csee.histudy.controller.form.BannerReorderForm;",
        "ReportService.java: import edu.handong.csee.histudy.controller.form.ReportForm;",
        "UserService.java: import edu.handong.csee.histudy.controller.form.ApplyForm;",
        "UserService.java: import edu.handong.csee.histudy.controller.form.UserForm;");
```

```java
@Test
void 서비스계층의_컨트롤러의존은_레거시기준선을_넘지않는다() throws IOException {
  // Given
  List<Path> serviceSources;
  try (Stream<Path> paths = Files.walk(SERVICE_SOURCE_DIRECTORY)) {
    serviceSources =
        paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
  }

  // When
  Set<String> controllerDependencies =
      serviceSources.stream()
          .flatMap(
              source ->
                  readLines(source)
                      .filter(LayerDependencyTest::isControllerDependency)
                      .map(line -> source.getFileName() + ": " + line))
          .collect(Collectors.toSet());

  // Then
  assertThat(controllerDependencies)
      .containsExactlyInAnyOrderElementsOf(LEGACY_SERVICE_CONTROLLER_DEPENDENCIES);
}
```

```java
private static boolean isControllerDependency(String line) {
  return isSourceCodeLine(line) && line.contains(CONTROLLER_PACKAGE_PREFIX);
}
```

- [ ] **Step 2: 아키텍처 테스트가 학기 form 의존 때문에 실패하는지 확인**

Run:

```bash
./gradlew test --tests "*LayerDependencyTest"
```

Expected: `AcademicTermService.java: import ...AcademicTermForm;`이 실제 집합에 추가되어 테스트가 실패한다.

- [ ] **Step 3: 학기 서비스가 스칼라 입력을 받도록 최소 변경**

`AcademicTermService`에서 `AcademicTermForm` import를 제거하고 메서드를 다음 형태로 변경한다.

```java
@Transactional
public void createAcademicTerm(Integer year, TermType semester) {
  academicTermRepository
      .findByYearAndTerm(year, semester)
      .ifPresent(
          existing -> {
            throw new DuplicateAcademicTermException(year, semester);
          });

  AcademicTerm academicTerm =
      AcademicTerm.builder()
          .academicYear(year)
          .semester(semester)
          .isCurrent(false)
          .build();

  academicTermRepository.save(academicTerm);
}
```

`AdminController`의 서비스 호출을 다음과 같이 변경한다.

```java
academicTermService.createAcademicTerm(form.getYear(), form.getSemester());
```

- [ ] **Step 4: 서비스·컨트롤러 테스트를 새 계약에 맞게 변경**

`AcademicTermServiceTest`에서 `AcademicTermForm` import와 fixture를 제거하고 다음과 같이 직접 값을 전달한다.

```java
academicTermService.createAcademicTerm(2025, TermType.FALL);
```

중복 검증도 다음 호출을 사용한다.

```java
assertThatThrownBy(() -> academicTermService.createAcademicTerm(2025, TermType.FALL))
    .isInstanceOf(DuplicateAcademicTermException.class);
```

`AdminControllerTest`의 성공 케이스는 정확한 값 전달을 검증한다.

```java
doNothing().when(academicTermService).createAcademicTerm(2025, TermType.SPRING);
verify(academicTermService).createAcademicTerm(2025, TermType.SPRING);
```

비관리자 케이스는 서비스가 호출되지 않음을 새 시그니처로 검증한다.

```java
verify(academicTermService, never()).createAcademicTerm(any(), any());
```

- [ ] **Step 5: 관련 테스트 GREEN 확인**

Run:

```bash
./gradlew test --tests "*LayerDependencyTest" --tests "*AcademicTermServiceTest" --tests "*AdminControllerTest"
```

Expected: 세 테스트 클래스가 모두 통과한다.

- [ ] **Step 6: 전체 회귀 검증**

Run:

```bash
./gradlew test --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`. 기존 deprecation 경고 외에 새 오류가 없다.

- [ ] **Step 7: 구현 커밋**

```bash
git add src/main/java/edu/handong/csee/histudy/service/AcademicTermService.java \
  src/main/java/edu/handong/csee/histudy/controller/AdminController.java \
  src/test/java/edu/handong/csee/histudy/architecture/LayerDependencyTest.java \
  src/test/java/edu/handong/csee/histudy/service/AcademicTermServiceTest.java \
  src/test/java/edu/handong/csee/histudy/controller/AdminControllerTest.java
git commit -m "refactor: 학기 서비스의 HTTP form 의존 제거" \
  -m "서비스 입력을 API 바인딩 타입과 분리하고 남은 역방향 의존이 증가하지 않도록 계층 기준선을 강화한다."
```
