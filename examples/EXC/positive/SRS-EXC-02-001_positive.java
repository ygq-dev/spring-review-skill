import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SRS_EXC_02_001_positive {
    private static final Logger log = LoggerFactory.getLogger(SRS_EXC_02_001_positive.class);

    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("doWork failed", e);
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}