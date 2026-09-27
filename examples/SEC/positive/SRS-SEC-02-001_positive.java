import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SafeLogService {
    private static final Logger log = LoggerFactory.getLogger(SafeLogService.class);

    public void login(String userId) {
        log.info("user login id={}", mask(userId));
    }

    private String mask(String value) {
        return value == null ? null : value.replaceAll("(\\d{3})\\d+(\\d{2})", "$1****$2");
    }
}