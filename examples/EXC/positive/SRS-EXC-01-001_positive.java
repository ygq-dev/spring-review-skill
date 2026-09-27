import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SRS_EXC_01_001_positive {
    private static final Logger log = LoggerFactory.getLogger(SRS_EXC_01_001_positive.class);

    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("handle failed", e);
            throw new IllegalStateException("handle failed", e);
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}