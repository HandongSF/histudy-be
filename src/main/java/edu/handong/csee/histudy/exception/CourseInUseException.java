package edu.handong.csee.histudy.exception;

public class CourseInUseException extends RuntimeException {

  public CourseInUseException() {
    super("사용 중인 강의는 삭제할 수 없습니다.");
  }
}
