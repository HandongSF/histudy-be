package edu.handong.csee.histudy.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LayerDependencyTest {

  private static final Path MAIN_SOURCE_DIRECTORY =
      Path.of("src/main/java/edu/handong/csee/histudy");
  private static final Path CONTROLLER_SOURCE_DIRECTORY =
      MAIN_SOURCE_DIRECTORY.resolve("controller");
  private static final Path SERVICE_SOURCE_DIRECTORY = MAIN_SOURCE_DIRECTORY.resolve("service");
  private static final Path MATCHING_DOMAIN_SOURCE_DIRECTORY =
      MAIN_SOURCE_DIRECTORY.resolve("matching/domain");
  private static final Path MATCHING_APPLICATION_SOURCE_DIRECTORY =
      MAIN_SOURCE_DIRECTORY.resolve("matching/application");
  private static final String CONTROLLER_PACKAGE_PREFIX =
      "edu.handong.csee.histudy.controller.";
  private static final String REPOSITORY_PACKAGE_PREFIX =
      "edu.handong.csee.histudy.repository.";
  private static final String MATCHING_APPLICATION_PACKAGE_PREFIX =
      "edu.handong.csee.histudy.matching.application.";
  private static final List<String> LEGACY_SERVICE_CONTROLLER_DEPENDENCIES = List.of();
  private static final List<String> FORBIDDEN_DEPENDENCY_PREFIXES =
      List.of(
          "edu.handong.csee.histudy.controller.",
          "edu.handong.csee.histudy.dto.",
          REPOSITORY_PACKAGE_PREFIX,
          "edu.handong.csee.histudy.service.");

  @Test
  void 도메인계층은_외부레이어에_의존하지_않는다() throws IOException {
    // Given
    List<Path> domainSources;
    try (Stream<Path> paths = Files.walk(MAIN_SOURCE_DIRECTORY)) {
      domainSources =
          paths
              .filter(path -> path.toString().endsWith(".java"))
              .filter(LayerDependencyTest::isDomainSource)
              .sorted()
              .toList();
    }

    // When
    List<String> forbiddenDependencies =
        domainSources.stream()
            .flatMap(
                source ->
                    readLines(source)
                        .filter(LayerDependencyTest::isForbiddenDependency)
                        .map(line -> source + ": " + line))
            .toList();

    // Then
    assertThat(forbiddenDependencies).isEmpty();
  }

  @Test
  void 매칭도메인은_Spring에_의존하지_않는다() throws IOException {
    // Given
    List<Path> matchingDomainSources;
    try (Stream<Path> paths = Files.walk(MATCHING_DOMAIN_SOURCE_DIRECTORY)) {
      matchingDomainSources =
          paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
    }

    // When
    List<String> springDependencies =
        matchingDomainSources.stream()
            .flatMap(
                source ->
                    readLines(source)
                        .filter(line -> isSourceCodeLine(line) && line.contains("org.springframework."))
                        .map(line -> source + ": " + line))
            .toList();

    // Then
    assertThat(springDependencies).isEmpty();
  }

  @Test
  void 매칭애플리케이션은_API와_레거시서비스에_의존하지_않는다() throws IOException {
    // Given
    List<Path> matchingApplicationSources;
    try (Stream<Path> paths = Files.walk(MATCHING_APPLICATION_SOURCE_DIRECTORY)) {
      matchingApplicationSources =
          paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
    }

    // When
    List<String> forbiddenDependencies =
        matchingApplicationSources.stream()
            .flatMap(
                source ->
                    readLines(source)
                        .filter(
                            line ->
                                isSourceCodeLine(line)
                                    && (line.contains(CONTROLLER_PACKAGE_PREFIX)
                                        || line.contains("edu.handong.csee.histudy.dto.")
                                        || line.contains("edu.handong.csee.histudy.service.")))
                        .map(line -> source + ": " + line))
            .toList();

    // Then
    assertThat(forbiddenDependencies).isEmpty();
  }

  @Test
  void 컨트롤러계층은_리포지토리에_직접의존하지_않는다() throws IOException {
    // Given
    List<Path> controllerSources;
    try (Stream<Path> paths = Files.walk(CONTROLLER_SOURCE_DIRECTORY)) {
      controllerSources =
          paths.filter(path -> path.toString().endsWith(".java")).sorted().toList();
    }

    // When
    List<String> forbiddenDependencies =
        controllerSources.stream()
            .flatMap(
                source ->
                    readLines(source)
                        .filter(LayerDependencyTest::isRepositoryDependency)
                        .map(line -> source + ": " + line))
            .toList();

    // Then
    assertThat(forbiddenDependencies).isEmpty();
  }

  @Test
  void 서비스계층의_컨트롤러의존은_레거시기준선을_넘지않는다() throws IOException {
    // Given When
    List<String> controllerDependencies =
        collectControllerDependencies(SERVICE_SOURCE_DIRECTORY);

    // Then
    assertThat(controllerDependencies)
        .containsExactlyInAnyOrderElementsOf(LEGACY_SERVICE_CONTROLLER_DEPENDENCIES);
  }

  @Test
  void 전역서비스는_매칭애플리케이션에_의존하지_않는다() throws IOException {
    // Given When
    List<String> matchingApplicationDependencies =
        collectDependencies(SERVICE_SOURCE_DIRECTORY, MATCHING_APPLICATION_PACKAGE_PREFIX);

    // Then
    assertThat(matchingApplicationDependencies).isEmpty();
  }

  @Test
  void 같은이름의_서비스소스가_서로다른패키지에있어도_컨트롤러의존을_모두탐지한다(
      @TempDir Path serviceDirectory) throws IOException {
    // Given
    String controllerImport =
        "import edu.handong.csee.histudy.controller.form.AcademicTermForm;";
    Path firstSource = serviceDirectory.resolve("first/SameService.java");
    Path secondSource = serviceDirectory.resolve("second/SameService.java");
    Files.createDirectories(firstSource.getParent());
    Files.createDirectories(secondSource.getParent());
    Files.writeString(firstSource, controllerImport);
    Files.writeString(secondSource, controllerImport);

    // When
    List<String> controllerDependencies = collectControllerDependencies(serviceDirectory);

    // Then
    assertThat(controllerDependencies)
        .containsExactlyInAnyOrder(
            "first/SameService.java: " + controllerImport,
            "second/SameService.java: " + controllerImport);
  }

  @Test
  void java확장자와_같은이름의_디렉터리는_서비스소스로_탐지하지않는다(
      @TempDir Path serviceDirectory) throws IOException {
    // Given
    Files.createDirectories(serviceDirectory.resolve("NotSource.java"));

    // When
    List<String> controllerDependencies = collectControllerDependencies(serviceDirectory);

    // Then
    assertThat(controllerDependencies).isEmpty();
  }

  @Test
  void FQCN으로_외부레이어를_참조하면_금지된의존성으로_탐지한다() {
    // Given
    String codeLine = "private edu.handong.csee.histudy.dto.UserDto userDto;";

    // When
    boolean forbiddenDependency = isForbiddenDependency(codeLine);

    // Then
    assertThat(forbiddenDependency).isTrue();
  }

  @Test
  void 주석에_리포지토리패키지명이_있으면_직접의존으로_탐지하지않는다() {
    // Given
    List<String> commentLines =
        List.of(
            "// " + REPOSITORY_PACKAGE_PREFIX,
            "/* " + REPOSITORY_PACKAGE_PREFIX,
            "* " + REPOSITORY_PACKAGE_PREFIX);

    // When
    List<String> repositoryDependencies =
        commentLines.stream().filter(LayerDependencyTest::isRepositoryDependency).toList();

    // Then
    assertThat(repositoryDependencies).isEmpty();
  }

  @Test
  void FQCN으로_리포지토리를_참조하면_직접의존으로_탐지한다() {
    // Given
    String codeLine =
        "private edu.handong.csee.histudy.repository.UserRepository userRepository;";

    // When
    boolean repositoryDependency = isRepositoryDependency(codeLine);

    // Then
    assertThat(repositoryDependency).isTrue();
  }

  @Test
  void 파일경로에_금지된패키지명이_있어도_코드가_참조하지않으면_탐지하지않는다(
      @TempDir Path directory) throws IOException {
    // Given
    Path source = directory.resolve("edu.handong.csee.histudy.service.Domain.java");
    Files.writeString(source, "private String name;");

    // When
    List<String> forbiddenDependencies =
        readLines(source).filter(LayerDependencyTest::isForbiddenDependency).toList();

    // Then
    assertThat(forbiddenDependencies).isEmpty();
  }

  private static Stream<String> readLines(Path source) {
    try {
      return Files.readAllLines(source).stream().map(String::trim);
    } catch (IOException e) {
      throw new IllegalStateException("도메인 소스 파일을 읽을 수 없습니다: " + source, e);
    }
  }

  private static List<String> collectControllerDependencies(Path serviceSourceDirectory)
      throws IOException {
    return collectDependencies(serviceSourceDirectory, CONTROLLER_PACKAGE_PREFIX);
  }

  private static List<String> collectDependencies(Path sourceDirectory, String packagePrefix)
      throws IOException {
    List<Path> serviceSources;
    try (Stream<Path> paths = Files.walk(sourceDirectory)) {
      serviceSources =
          paths
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".java"))
              .sorted()
              .toList();
    }

    return serviceSources.stream()
        .flatMap(
            source ->
                readLines(source)
                    .filter(line -> isSourceCodeLine(line) && line.contains(packagePrefix))
                    .map(
                        line ->
                            relativeSourcePath(sourceDirectory, source) + ": " + line))
        .toList();
  }

  private static String relativeSourcePath(Path sourceDirectory, Path source) {
    return sourceDirectory
        .relativize(source)
        .toString()
        .replace(source.getFileSystem().getSeparator(), "/");
  }

  private static boolean isForbiddenDependency(String line) {
    return isSourceCodeLine(line)
        && FORBIDDEN_DEPENDENCY_PREFIXES.stream().anyMatch(line::contains);
  }

  private static boolean isRepositoryDependency(String line) {
    return isSourceCodeLine(line) && line.contains(REPOSITORY_PACKAGE_PREFIX);
  }

  private static boolean isSourceCodeLine(String line) {
    return !line.startsWith("//") && !line.startsWith("/*") && !line.startsWith("*");
  }

  private static boolean isDomainSource(Path source) {
    return Stream.iterate(source, path -> path != null, Path::getParent)
        .map(Path::getFileName)
        .anyMatch(path -> path != null && path.toString().equals("domain"));
  }
}
