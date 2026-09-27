package evaluation.defects;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public class DEFECT_014 {
    public void configure(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable());
    }
}
