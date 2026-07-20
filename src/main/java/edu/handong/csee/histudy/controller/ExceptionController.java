package edu.handong.csee.histudy.controller;

import edu.handong.csee.histudy.dto.ExceptionResponse;
import edu.handong.csee.histudy.dto.UserDto;
import edu.handong.csee.histudy.exception.AcademicTermNotFoundException;
import edu.handong.csee.histudy.exception.BannerNotFoundException;
import edu.handong.csee.histudy.exception.CourseInUseException;
import edu.handong.csee.histudy.exception.CourseNotFoundException;
import edu.handong.csee.histudy.exception.DuplicateAcademicTermException;
import edu.handong.csee.histudy.exception.ForbiddenException;
import edu.handong.csee.histudy.exception.MissingEmailException;
import edu.handong.csee.histudy.exception.MissingParameterException;
import edu.handong.csee.histudy.exception.MissingSubException;
import edu.handong.csee.histudy.exception.MissingTokenException;
import edu.handong.csee.histudy.exception.NoCurrentTermFoundException;
import edu.handong.csee.histudy.exception.NoStudyApplicationFound;
import edu.handong.csee.histudy.exception.ReportNotFoundException;
import edu.handong.csee.histudy.exception.StudyGroupNotFoundException;
import edu.handong.csee.histudy.exception.UserAlreadyExistsException;
import edu.handong.csee.histudy.exception.UserNotFoundException;
import edu.handong.csee.histudy.service.DiscordService;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class ExceptionController {

  private final DiscordService discordService;

  private ResponseEntity<ExceptionResponse> createErrorResponse(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(ExceptionResponse.builder().status(status).message(message).build());
  }

  @ExceptionHandler({
    MissingParameterException.class,
    MissingEmailException.class,
    MissingSubException.class
  })
  public ResponseEntity<ExceptionResponse> handleBadRequest(Exception e) {
    return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ExceptionResponse> handleHttpMessageNotReadable(
      HttpMessageNotReadableException e) {
    return createErrorResponse(HttpStatus.BAD_REQUEST, "Invalid request format");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  public ResponseEntity<ExceptionResponse> handleMaxUploadSizeExceeded(
      MaxUploadSizeExceededException e) {
    return createErrorResponse(
        HttpStatus.PAYLOAD_TOO_LARGE, "업로드 가능한 파일 용량을 초과했습니다.");
  }

  @ExceptionHandler({JwtException.class, MissingTokenException.class})
  public ResponseEntity<ExceptionResponse> handleUnauthorized(Exception e) {
    return createErrorResponse(HttpStatus.UNAUTHORIZED, e.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<ExceptionResponse> handleForbidden(ForbiddenException e) {
    return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage());
  }

  @ExceptionHandler({
    AcademicTermNotFoundException.class,
    BannerNotFoundException.class,
    CourseNotFoundException.class,
    StudyGroupNotFoundException.class,
    ReportNotFoundException.class,
    NoCurrentTermFoundException.class,
    NoStudyApplicationFound.class
  })
  public ResponseEntity<ExceptionResponse> handleNotFound(Exception e) {
    return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
  }

  @ExceptionHandler(UserNotFoundException.class)
  public ResponseEntity<UserDto.UserLogin> handleUserNotFound() {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(UserDto.UserLogin.builder().isRegistered(false).build());
  }

  @ExceptionHandler({
    CourseInUseException.class,
    DuplicateAcademicTermException.class,
    UserAlreadyExistsException.class
  })
  public ResponseEntity<ExceptionResponse> handleConflict(Exception e) {
    return createErrorResponse(HttpStatus.CONFLICT, e.getMessage());
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ExceptionResponse> runtimeException(Exception e, WebRequest request) {
    log.error("Unhandled Exception Occurred", e);
    discordService.notifyException(e, request);
    return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
  }
}
