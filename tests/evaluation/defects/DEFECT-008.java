package evaluation.defects;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DEFECT_008 {
    @PostMapping("/users")
    public String create(@RequestBody UserRequest request) {
        return request.getName();
    }

    static class UserRequest {
        private String name;

        public String getName() {
            return name;
        }
    }
}
