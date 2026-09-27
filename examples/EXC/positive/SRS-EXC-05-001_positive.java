import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

class SRS_EXC_05_001_positive {
    private static final Logger log = LoggerFactory.getLogger(SRS_EXC_05_001_positive.class);

    @Async
    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            log.error("async task failed", e);
        }
    }

    private void doWork() {
        throw new IllegalStateException("bad");
    }
}