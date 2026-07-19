# Safe Course Deletion API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 관리자가 현재 학기의 미사용 과목만 `DELETE /api/courses/{courseId}`로 안전하게 삭제할 수 있게 한다.

**Architecture:** 기존 `CourseController -> CourseService -> CourseRepository` 흐름을 유지한다. 서비스가 현재 학기 및 참조 여부를 결정하고, JPA 구현은 세 연관 엔티티의 참조 존재 여부를 조회하며, 기존 `POST /api/courses/delete` 계약은 변경하지 않는다.

**Tech Stack:** Java 17, Spring Boot 3.5, Spring MVC, Spring Data JPA, JUnit 5, AssertJ, Mockito, H2, OpenAPI 3.0

## Global Constraints

- 사용 중인 과목은 삭제하지 않고 `409 Conflict`를 반환한다.
- 현재 학기가 아닌 과목은 `404 Not Found`로 처리한다.
- 성공 응답은 본문 없는 `204 No Content`다.
- 기존 `POST /api/courses/delete` 및 `CourseIdDto` 동작은 유지한다.
- 컨트롤러는 권한과 HTTP 변환만 담당하고 비즈니스 결정은 서비스에 둔다.
- 신청, 그룹, 리포트 및 과거 학기 데이터를 연쇄 삭제하지 않는다.

---

### Task 1: 과목 참조 조회 계약

**Files:**
- Modify: `src/main/java/edu/handong/csee/histudy/repository/CourseRepository.java`
- Modify: `src/main/java/edu/handong/csee/histudy/repository/jpa/JpaCourseRepository.java`
- Modify: `src/main/java/edu/handong/csee/histudy/repository/impl/CourseRepositoryImpl.java`
- Modify: `src/test/java/edu/handong/csee/histudy/service/repository/fake/FakeCourseRepository.java`
- Create: `src/test/java/edu/handong/csee/histudy/repository/impl/CourseRepositoryImplTest.java`

**Interfaces:**
- Consumes: `PreferredCourse.course`, `GroupCourse.course`, `StudyCourse.course` JPA 관계
- Produces: `CourseRepository.hasReferences(Long courseId): boolean`

- [ ] **Step 1: 참조가 없는 과목을 판별하는 실패 테스트 작성**

```java
@DataJpaTest
@Import(CourseRepositoryImpl.class)
class CourseRepositoryImplTest {

  @Autowired private TestEntityManager entityManager;
  @Autowired private CourseRepository courseRepository;

  private AcademicTerm currentTerm;
  private Course course;

  @BeforeEach
  void setUp() {
    currentTerm =
        entityManager.persist(
            AcademicTerm.builder()
                .academicYear(2026)
                .semester(TermType.SUMMER)
                .isCurrent(true)
                .build());
    course =
        entityManager.persistAndFlush(
            Course.builder()
                .name("자료구조")
                .code("CSEE201")
                .professor("Kim")
                .academicTerm(currentTerm)
                .build());
  }

  @Test
  void 참조가_없는_과목을_조회하면_사용중이_아니다() {
    // Given
    // When
    boolean result = courseRepository.hasReferences(course.getCourseId());

    // Then
    assertThat(result).isFalse();
  }
}
```

- [ ] **Step 2: 테스트가 인터페이스 부재로 실패하는지 확인**

Run: `./gradlew test --tests '*CourseRepositoryImplTest.참조가_없는_과목을_조회하면_사용중이_아니다'`

Expected: `CourseRepository.hasReferences`를 찾을 수 없어 컴파일 실패

- [ ] **Step 3: 저장소 참조 조회를 최소 구현**

`CourseRepository`에 다음을 추가한다.

```java
boolean hasReferences(Long courseId);
```

`JpaCourseRepository`에 다음을 추가한다.

```java
@Query("select count(pc) from PreferredCourse pc where pc.course.courseId = :courseId")
long countPreferredCourseReferences(@Param("courseId") Long courseId);

@Query("select count(gc) from GroupCourse gc where gc.course.courseId = :courseId")
long countGroupCourseReferences(@Param("courseId") Long courseId);

@Query("select count(sc) from StudyCourse sc where sc.course.courseId = :courseId")
long countStudyCourseReferences(@Param("courseId") Long courseId);
```

`CourseRepositoryImpl`에 다음을 추가한다.

```java
@Override
public boolean hasReferences(Long courseId) {
  return repository.countPreferredCourseReferences(courseId) > 0
      || repository.countGroupCourseReferences(courseId) > 0
      || repository.countStudyCourseReferences(courseId) > 0;
}
```

`FakeCourseRepository`에는 다음 테스트 더블 동작을 추가한다.

```java
private final Set<Long> referencedCourseIds = new HashSet<>();

@Override
public boolean hasReferences(Long courseId) {
  return referencedCourseIds.contains(courseId);
}

public void markReferenced(Long courseId) {
  referencedCourseIds.add(courseId);
}
```

- [ ] **Step 4: 참조 없음 테스트가 통과하는지 확인**

Run: `./gradlew test --tests '*CourseRepositoryImplTest.참조가_없는_과목을_조회하면_사용중이_아니다'`

Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: 신청·그룹·리포트 참조 실패 테스트 추가**

```java
@Test
void 신청이_과목을_참조하면_사용중이다() {
  // Given
  User user =
      entityManager.persist(
          User.builder()
              .sub("sub-1")
              .sid("22230001")
              .email("user@histudy.com")
              .name("User")
              .role(Role.USER)
              .build());
  entityManager.persistAndFlush(StudyApplicant.of(currentTerm, user, List.of(), List.of(course)));

  // When
  boolean result = courseRepository.hasReferences(course.getCourseId());

  // Then
  assertThat(result).isTrue();
}

@Test
void 그룹이_과목을_참조하면_사용중이다() {
  // Given
  StudyGroup group = StudyGroup.of(1, currentTerm, List.of());
  new GroupCourse(course, group);
  entityManager.persistAndFlush(group);

  // When
  boolean result = courseRepository.hasReferences(course.getCourseId());

  // Then
  assertThat(result).isTrue();
}

@Test
void 리포트가_과목을_참조하면_사용중이다() {
  // Given
  StudyGroup group = entityManager.persist(StudyGroup.of(1, currentTerm, List.of()));
  StudyReport report =
      StudyReport.builder()
          .title("1주차")
          .content("학습 내용")
          .totalMinutes(60)
          .studyGroup(group)
          .participants(List.of())
          .images(List.of())
          .courses(List.of(course))
          .build();
  entityManager.persistAndFlush(report);

  // When
  boolean result = courseRepository.hasReferences(course.getCourseId());

  // Then
  assertThat(result).isTrue();
}
```

- [ ] **Step 6: 저장소 테스트 전체 통과 확인**

Run: `./gradlew test --tests '*CourseRepositoryImplTest'`

Expected: 4 tests pass, `BUILD SUCCESSFUL`

- [ ] **Step 7: 커밋**

```bash
git add src/main/java/edu/handong/csee/histudy/repository/CourseRepository.java src/main/java/edu/handong/csee/histudy/repository/jpa/JpaCourseRepository.java src/main/java/edu/handong/csee/histudy/repository/impl/CourseRepositoryImpl.java src/test/java/edu/handong/csee/histudy/service/repository/fake/FakeCourseRepository.java src/test/java/edu/handong/csee/histudy/repository/impl/CourseRepositoryImplTest.java
git commit -m "feat: detect course references before deletion"
```

### Task 2: 현재 학기 과목 삭제 정책

**Files:**
- Modify: `src/main/java/edu/handong/csee/histudy/service/CourseService.java`
- Modify: `src/test/java/edu/handong/csee/histudy/service/CourseServiceTest.java`
- Create: `src/main/java/edu/handong/csee/histudy/exception/CourseInUseException.java`

**Interfaces:**
- Consumes: `CourseRepository.findById`, `CourseRepository.hasReferences`, `CourseRepository.deleteById`
- Produces: `CourseService.deleteCurrentCourse(Long courseId): void`, `CourseInUseException`

- [ ] **Step 1: 서비스 정책 실패 테스트 작성**

```java
@Test
void 현재_학기_미사용_과목을_삭제하면_과목이_제거된다() {
  // Given
  Course savedCourse = courseRepository.saveAll(List.of(currentCourse)).get(0);

  // When
  courseService.deleteCurrentCourse(savedCourse.getCourseId());

  // Then
  assertThat(courseRepository.findAll()).isEmpty();
}

@Test
void 없는_과목을_삭제하면_예외가_발생한다() {
  // Given
  // When Then
  assertThatThrownBy(() -> courseService.deleteCurrentCourse(999L))
      .isInstanceOf(CourseNotFoundException.class);
}

@Test
void 과거_학기_과목을_삭제하면_예외가_발생한다() {
  // Given
  Course savedCourse = courseRepository.saveAll(List.of(previousCourse)).get(0);

  // When Then
  assertThatThrownBy(() -> courseService.deleteCurrentCourse(savedCourse.getCourseId()))
      .isInstanceOf(CourseNotFoundException.class);
}

@Test
void 사용중인_과목을_삭제하면_예외가_발생한다() {
  // Given
  Course savedCourse = courseRepository.saveAll(List.of(currentCourse)).get(0);
  courseRepository.markReferenced(savedCourse.getCourseId());

  // When Then
  assertThatThrownBy(() -> courseService.deleteCurrentCourse(savedCourse.getCourseId()))
      .isInstanceOf(CourseInUseException.class)
      .hasMessage("사용 중인 강의는 삭제할 수 없습니다.");
  assertThat(courseRepository.findAll()).containsExactly(savedCourse);
}
```

- [ ] **Step 2: 서비스 메서드 부재로 실패하는지 확인**

Run: `./gradlew test --tests '*CourseServiceTest'`

Expected: `deleteCurrentCourse` 및 `CourseInUseException`을 찾을 수 없어 컴파일 실패

- [ ] **Step 3: 예외와 서비스 정책을 최소 구현**

```java
public class CourseInUseException extends RuntimeException {

  public CourseInUseException() {
    super("사용 중인 강의는 삭제할 수 없습니다.");
  }
}
```

```java
@Transactional
public void deleteCurrentCourse(Long courseId) {
  Course course =
      courseRepository.findById(courseId).orElseThrow(CourseNotFoundException::new);
  if (!Boolean.TRUE.equals(course.getAcademicTerm().getIsCurrent())) {
    throw new CourseNotFoundException();
  }
  if (courseRepository.hasReferences(courseId)) {
    throw new CourseInUseException();
  }
  courseRepository.deleteById(courseId);
}
```

- [ ] **Step 4: 서비스 테스트 통과 확인**

Run: `./gradlew test --tests '*CourseServiceTest'`

Expected: 기존 테스트와 신규 4개 테스트 pass, `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/edu/handong/csee/histudy/service/CourseService.java src/main/java/edu/handong/csee/histudy/exception/CourseInUseException.java src/test/java/edu/handong/csee/histudy/service/CourseServiceTest.java
git commit -m "feat: protect current course deletion"
```

### Task 3: 관리자 DELETE HTTP 계약

**Files:**
- Modify: `src/main/java/edu/handong/csee/histudy/controller/CourseController.java`
- Modify: `src/main/java/edu/handong/csee/histudy/controller/ExceptionController.java`
- Modify: `src/test/java/edu/handong/csee/histudy/controller/CourseControllerTest.java`

**Interfaces:**
- Consumes: `CourseService.deleteCurrentCourse(Long)` 및 기존 권한 검사
- Produces: `DELETE /api/courses/{courseId}`와 `204`, `403`, `404`, `409` 상태 계약

- [ ] **Step 1: 컨트롤러 실패 테스트 작성**

```java
@Test
void 관리자가_현재학기_강의삭제시_본문없이_성공한다() throws Exception {
  // Given
  Claims claims = adminClaims("admin@test.com");

  // When Then
  mockMvc
      .perform(delete("/api/courses/{courseId}", 1L).requestAttr("claims", claims))
      .andExpect(status().isNoContent())
      .andExpect(content().string(""));
  verify(courseService).deleteCurrentCourse(1L);
}

@Test
void 권한없는사용자가_현재학기_강의삭제시_실패한다() throws Exception {
  // Given
  Claims claims = userClaims("user@test.com");

  // When Then
  mockMvc
      .perform(delete("/api/courses/{courseId}", 1L).requestAttr("claims", claims))
      .andExpect(status().isForbidden());
  verify(courseService, never()).deleteCurrentCourse(anyLong());
}

@Test
void 없는_강의삭제시_찾을수없음으로_응답한다() throws Exception {
  // Given
  Claims claims = adminClaims("admin@test.com");
  doThrow(new CourseNotFoundException()).when(courseService).deleteCurrentCourse(1L);

  // When Then
  mockMvc
      .perform(delete("/api/courses/{courseId}", 1L).requestAttr("claims", claims))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("해당 강의를 찾을 수 없습니다."));
}

@Test
void 사용중인_강의삭제시_충돌로_응답한다() throws Exception {
  // Given
  Claims claims = adminClaims("admin@test.com");
  doThrow(new CourseInUseException()).when(courseService).deleteCurrentCourse(1L);

  // When Then
  mockMvc
      .perform(delete("/api/courses/{courseId}", 1L).requestAttr("claims", claims))
      .andExpect(status().isConflict())
      .andExpect(jsonPath("$.message").value("사용 중인 강의는 삭제할 수 없습니다."));
}
```

- [ ] **Step 2: 신규 DELETE 경로 부재로 실패하는지 확인**

Run: `./gradlew test --tests '*CourseControllerTest'`

Expected: 신규 요청이 `405 Method Not Allowed` 또는 컴파일 대상 import 부재로 실패

- [ ] **Step 3: 컨트롤러와 충돌 매핑 최소 구현**

`CourseController`에 추가한다.

```java
@DeleteMapping("/{courseId}")
public ResponseEntity<Void> deleteCurrentCourse(
    @PathVariable Long courseId, @RequestAttribute Claims claims) {
  if (Role.isAuthorized(claims, Role.ADMIN)) {
    courseService.deleteCurrentCourse(courseId);
    return ResponseEntity.noContent().build();
  }
  throw new ForbiddenException();
}
```

`ExceptionController.handleConflict` 대상에 `CourseInUseException.class`를 추가한다.

- [ ] **Step 4: 컨트롤러 테스트 통과 확인**

Run: `./gradlew test --tests '*CourseControllerTest'`

Expected: 기존 및 신규 테스트 pass, `BUILD SUCCESSFUL`

- [ ] **Step 5: 커밋**

```bash
git add src/main/java/edu/handong/csee/histudy/controller/CourseController.java src/main/java/edu/handong/csee/histudy/controller/ExceptionController.java src/test/java/edu/handong/csee/histudy/controller/CourseControllerTest.java
git commit -m "feat: expose safe course deletion API"
```

### Task 4: 계약과 도메인 문서 동기화

**Files:**
- Modify: `api-docs.yaml`
- Modify: `docs/domain.md`

**Interfaces:**
- Consumes: 구현된 `DELETE /api/courses/{courseId}` 동작
- Produces: 프런트엔드가 사용할 OpenAPI 계약과 지속적인 삭제 불변 조건

- [ ] **Step 1: OpenAPI 경로와 오류 스키마 추가**

`api-docs.yaml`의 버전을 `v1.3.2`로 올리고 다음 경로를 추가한다.

```yaml
  /api/courses/{courseId}:
    delete:
      tags:
        - 강의 관리 API
      summary: 현재 학기 강의 삭제
      description: 현재 학기의 미사용 강의만 삭제합니다.
      operationId: deleteCurrentCourse
      parameters:
        - name: courseId
          in: path
          required: true
          schema:
            type: integer
            format: int64
      responses:
        '204':
          description: No Content
        '403':
          description: Forbidden
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ExceptionResponse'
        '404':
          description: Course not found in the current academic term
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ExceptionResponse'
        '409':
          description: Course is referenced by an application, group, or report
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ExceptionResponse'
```

`components.schemas`에 실제 응답 형태를 추가한다.

```yaml
    ExceptionResponse:
      type: object
      properties:
        code:
          type: integer
          format: int32
          example: 409
        error:
          type: string
          example: Conflict
        message:
          type: string
          example: 사용 중인 강의는 삭제할 수 없습니다.
        trace:
          type: string
          nullable: true
```

- [ ] **Step 2: 도메인 불변 조건 추가**

`docs/domain.md`의 운영 불변 조건에 다음을 추가한다.

```markdown
- 관리자는 현재 학기의 미사용 과목만 삭제할 수 있습니다. 신청, 그룹 또는 리포트가 참조하는 과목과 과거 학기 과목은 보존합니다.
```

- [ ] **Step 3: 문서 구문과 변경 범위 검증**

Run: `ruby -e "require 'yaml'; YAML.load_file('api-docs.yaml'); puts 'VALID'"`

Expected: `VALID`

Run: `git diff --check`

Expected: 출력 없음, exit 0

- [ ] **Step 4: 커밋**

```bash
git add api-docs.yaml docs/domain.md
git commit -m "docs: document course deletion responses"
```

### Task 5: 전체 회귀 검증

**Files:**
- Verify only: all changed production, test, and documentation files

**Interfaces:**
- Consumes: Tasks 1-4의 모든 결과
- Produces: PR에 첨부할 검증 근거

- [ ] **Step 1: 과목 관련 테스트를 새로 실행**

Run: `./gradlew test --tests '*CourseRepositoryImplTest' --tests '*CourseServiceTest' --tests '*CourseControllerTest'`

Expected: `BUILD SUCCESSFUL`, failures 0

- [ ] **Step 2: 전체 테스트를 깨끗한 빌드에서 실행**

Run: `./gradlew clean test`

Expected: `BUILD SUCCESSFUL`, failures 0

- [ ] **Step 3: 최종 diff와 계약 요구사항 확인**

Run: `git diff --check origin/main...HEAD`

Expected: 출력 없음, exit 0

Run: `git status -sb`

Expected: `agent/course-deletion-api` 브랜치, tracked working tree clean

- [ ] **Step 4: PR 게시 준비**

브랜치를 fork remote에 push하고, `HandongSF/histudy-be:main`을 대상으로 draft PR을 만든다. PR 본문에는 이슈 맥락, 신규 API 계약, 참조 데이터 보호 정책, 기존 API 호환성, 실행한 검증 명령을 기록한다.
