import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
class SRS_EXC_04_001_negative {
    @ExceptionHandler(Exception.class)
    ResponseEntity<Map<String, Object>> handle(Exception e) {
        return ResponseEntity.internalServerError()
                .body(Map.<String, Object>of("message", e.getMessage(), "stack", e.getStackTrace()));
    }
}