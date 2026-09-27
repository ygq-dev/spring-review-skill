package examples.web.negative;

import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AsyncController {
    @Async
    @GetMapping("/async")
    public void run() {
        throw new IllegalStateException("async failure");
    }
}