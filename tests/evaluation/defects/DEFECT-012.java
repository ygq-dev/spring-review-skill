package evaluation.defects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DEFECT_012 {
    private static final Logger log = LoggerFactory.getLogger(DEFECT_012.class);

    public void login(String password, String token) {
        log.info("login password={} token={}", password, token);
    }
}
