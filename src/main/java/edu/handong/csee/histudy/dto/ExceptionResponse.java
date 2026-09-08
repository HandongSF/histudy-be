package edu.handong.csee.histudy.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
public class ExceptionResponse {
    private Integer code;
    private String error;
    private String message;
    private String requestId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String errorId;

    @Builder
    public ExceptionResponse(HttpStatus status, String message, String requestId, String errorId) {
        this.code = status.value();
        this.error = status.getReasonPhrase();
        this.message = message;
        this.requestId = requestId;
        this.errorId = errorId;
    }
}
