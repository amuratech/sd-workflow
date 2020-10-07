package com.kylas.sales.workflow.error;

import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
@Slf4j
public class WebExceptionHandler extends ResponseEntityExceptionHandler {

  @ExceptionHandler(value = {ResourceNotFoundException.class})
  public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
      Exception ex, WebRequest request) {
    var errorResponse = new ErrorResponse();
    errorResponse.setTimestamp(LocalDateTime.now());
    errorResponse.setErrorCode(ex.getMessage());

    return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
  }
}
