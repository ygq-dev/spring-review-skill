package evaluation.clean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLEAN_005 {
    private static final Logger log = LoggerFactory.getLogger(CLEAN_005.class);

    public void logUser(String userId) {
        log.info("user={}", userId);
    }
}
