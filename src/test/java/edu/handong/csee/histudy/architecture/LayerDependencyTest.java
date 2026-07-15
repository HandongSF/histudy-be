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
  private static final String REPOSITORY_PACKAGE_PREFIX =
      "edu.handong.csee.histudy.repository.";
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
                        .filter(line -> line.contains(REPOSITORY_PACKAGE_PREFIX))
                        .map(line -> source + ": " + line))
            .toList();

    // Then
    assertThat(forbiddenDependencies).isEmpty();
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

  private static boolean isForbiddenDependency(String line) {
    return FORBIDDEN_DEPENDENCY_PREFIXES.stream().anyMatch(line::contains);
  }

  private static boolean isDomainSource(Path source) {
    return Stream.iterate(source, path -> path != null, Path::getParent)
        .map(Path::getFileName)
        .anyMatch(path -> path != null && path.toString().equals("domain"));
  }
}
