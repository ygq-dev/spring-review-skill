package evaluation.defects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DEFECT_018 {
    private static final Logger log = LoggerFactory.getLogger(DEFECT_018.class);

    public void logUser(String userId) {
        log.info("user=" + userId);
    }
}
