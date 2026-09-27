import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SensitiveLogService {
    private static final Logger log = LoggerFactory.getLogger(SensitiveLogService.class);

    public void login(String userId, String password, String token) {
        log.info("user login password={}, token={}", password, token);
        log.info("idCard=110101199001011234");
    }
}