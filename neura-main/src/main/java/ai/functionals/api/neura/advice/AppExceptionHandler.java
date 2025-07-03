package ai.functionals.api.neura.advice;

import ai.functionals.api.neura.model.commons.AppException;
import ai.functionals.api.neura.model.rsp.FaultRsp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<FaultRsp> handleAppException(AppException ex, WebRequest request) {
        log.error("App exception occurred: {}", ex.getMessage(), ex);
        
        FaultRsp faultRsp = FaultRsp.builder()
                .fault(ex.getMessage())
                .position(buildTraceList(ex.getStackTrace()))
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(faultRsp);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<FaultRsp> handleRuntimeException(RuntimeException ex, WebRequest request) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        
        // Extract the root cause message, especially for wrapped exceptions
        String faultMessage = extractRootCauseMessage(ex);
        
        FaultRsp faultRsp = FaultRsp.builder()
                .fault(faultMessage)
                .position(buildTraceList(ex.getStackTrace()))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(faultRsp);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<FaultRsp> handleIllegalArgumentException(IllegalArgumentException ex, WebRequest request) {
        log.error("Illegal argument exception occurred: {}", ex.getMessage(), ex);
        
        FaultRsp faultRsp = FaultRsp.builder()
                .fault(ex.getMessage())
                .position(buildTraceList(ex.getStackTrace()))
                .build();
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(faultRsp);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<FaultRsp> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);
        
        FaultRsp faultRsp = FaultRsp.builder()
                .fault("An unexpected error occurred: " + ex.getMessage())
                .position(buildTraceList(ex.getStackTrace()))
                .build();
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(faultRsp);
    }

    private String extractRootCauseMessage(RuntimeException ex) {
        // If the exception has a cause and the message is the generic "Failed to process design request",
        // try to get the more specific message from the root cause
        if (ex.getCause() != null && "Failed to process design request".equals(ex.getMessage())) {
            Throwable rootCause = getRootCause(ex);
            if (rootCause != null && rootCause.getMessage() != null && !rootCause.getMessage().isEmpty()) {
                return rootCause.getMessage();
            }
        }
        
        // For specific credit-related messages, always prefer the root cause
        if (ex.getCause() != null && ex.getMessage() != null && 
            (ex.getMessage().contains("Failed to process") || ex.getMessage().contains("failed"))) {
            Throwable rootCause = getRootCause(ex);
            if (rootCause != null && rootCause.getMessage() != null && 
                rootCause.getMessage().contains("credits")) {
                return rootCause.getMessage();
            }
        }
        
        return ex.getMessage();
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    private List<FaultRsp.Trace> buildTraceList(StackTraceElement[] stackTrace) {
        return Arrays.stream(stackTrace)
                .limit(10) // Limit to first 10 stack trace elements to avoid overly verbose responses
                .map(element -> FaultRsp.Trace.builder()
                        .fileName(element.getFileName())
                        .methodName(element.getMethodName())
                        .lineNumber(element.getLineNumber())
                        .build())
                .collect(Collectors.toList());
    }
}
