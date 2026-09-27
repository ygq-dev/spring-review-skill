package examples.web.positive;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.WebAsyncTask;

@RestController
public class AsyncController {
    @GetMapping("/async")
    public WebAsyncTask<String> run() {
        WebAsyncTask<String> task = new WebAsyncTask<>(5000L, () -> "ok");
        task.onTimeout(() -> "timeout");
        task.onError(() -> "error");
        return task;
    }
}