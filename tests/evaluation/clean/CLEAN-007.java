package evaluation.clean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLEAN_007 {
    private static final Logger log = LoggerFactory.getLogger(CLEAN_007.class);

    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("fail", e);
        }
    }

    private void doWork() {
    }
}
