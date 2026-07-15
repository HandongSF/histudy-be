package edu.handong.csee.histudy.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class LayerDependencyTest {

  private static final Path MAIN_SOURCE_DIRECTORY =
      Path.of("src/main/java/edu/handong/csee/histudy");
  private static final List<String> FORBIDDEN_DEPENDENCY_PREFIXES =
      List.of(
          "import edu.handong.csee.histudy.controller",
          "import edu.handong.csee.histudy.dto",
          "import edu.handong.csee.histudy.repository",
          "import edu.handong.csee.histudy.service");

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
    List<String> forbiddenImports =
        domainSources.stream()
            .flatMap(LayerDependencyTest::readLines)
            .filter(LayerDependencyTest::isForbiddenDependency)
            .toList();

    // Then
    assertThat(forbiddenImports).isEmpty();
  }

  private static Stream<String> readLines(Path source) {
    try {
      return Files.readAllLines(source).stream().map(line -> source + ": " + line.trim());
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
