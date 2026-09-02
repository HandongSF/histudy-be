package edu.handong.csee.histudy.util;

import edu.handong.csee.histudy.domain.AcademicTerm;
import edu.handong.csee.histudy.domain.Course;
import edu.handong.csee.histudy.exception.InvalidCourseCsvException;
import lombok.Builder;
import org.apache.commons.csv.CSVRecord;

@Builder
public class CourseCSV {
  private String title;
  private String code;
  private String professor;

  public static CourseCSV of(CSVRecord record) {
    String title = validateAndTrim(record.get("title"), "title", record.getRecordNumber());
    String code = validateAndTrim(record.get("code"), "code", record.getRecordNumber());
    String professor = validateAndTrim(record.get("prof"), "prof", record.getRecordNumber());

    return CourseCSV.builder().title(title).code(code).professor(professor).build();
  }

  private static String validateAndTrim(String value, String fieldName, long recordNumber) {
    String trimmed = value != null ? value.trim() : null;
    if (trimmed == null || trimmed.isEmpty()) {
      throw new InvalidCourseCsvException(
          "CSV 데이터의 " + fieldName + " 값은 비어 있을 수 없습니다. (행 " + recordNumber + ")");
    }
    return trimmed;
  }

  public Course toCourse(AcademicTerm academicTerm) {
    return Course.builder()
        .name(title)
        .code(code)
        .professor(professor)
        .academicTerm(academicTerm)
        .build();
  }
}
