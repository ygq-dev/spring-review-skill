package examples.web.negative;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    @PostMapping("/users")
    public String create(@RequestBody UserRequest request) {
        return request.getName();
    }
}

class UserRequest {
    private String name;
    public String getName() { return name; }
}