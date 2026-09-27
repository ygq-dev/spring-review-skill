package tests.fixtures.tcweb001;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {
    private String currentUser;

    @GetMapping("/user")
    public String getUser() {
        currentUser = "request-user";
        return currentUser;
    }
}