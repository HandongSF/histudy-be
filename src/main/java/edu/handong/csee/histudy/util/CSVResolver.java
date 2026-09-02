package edu.handong.csee.histudy.util;

import edu.handong.csee.histudy.exception.InvalidCourseCsvException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.input.BOMInputStream;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
public class CSVResolver {

  private static final List<String> REQUIRED_HEADERS = List.of("title", "code", "prof");

  private final List<CSVRecord> records;

  public static CSVResolver of(MultipartFile file) {
    try (InputStream in = file.getInputStream();
        Reader reader =
            new InputStreamReader(
                new BOMInputStream.Builder().setInputStream(in).get(), StandardCharsets.UTF_8);
        CSVParser parser =
            CSVParser.parse(
                reader,
                CSVFormat.Builder.create(CSVFormat.DEFAULT)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build())) {
      if (!REQUIRED_HEADERS.equals(parser.getHeaderNames())) {
        throw new InvalidCourseCsvException("CSV 헤더는 title,code,prof 형식이어야 합니다.");
      }

      List<CSVRecord> records = parser.getRecords();
      if (records.isEmpty()) {
        throw new InvalidCourseCsvException("CSV 파일에 수업 데이터가 없습니다.");
      }
      if (records.stream().anyMatch(record -> record.size() != REQUIRED_HEADERS.size())) {
        throw new InvalidCourseCsvException("CSV 데이터는 title,code,prof 세 열만 허용합니다.");
      }
      return new CSVResolver(records);
    } catch (InvalidCourseCsvException e) {
      throw e;
    } catch (IOException | IllegalArgumentException e) {
      throw new InvalidCourseCsvException("CSV 파일 형식이 올바르지 않습니다.");
    }
  }

  public List<CourseCSV> resolve() {
    return records.stream().map(CourseCSV::of).toList();
  }
}
