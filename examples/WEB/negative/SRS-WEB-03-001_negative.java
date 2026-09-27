package examples.web.negative;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ErrorController {
    @GetMapping("/error-demo")
    public String error() {
        try {
            throw new IllegalStateException("boom");
        } catch (Exception e) {
            return e.getMessage() + "\n" + e.getStackTrace();
        }
    }
}