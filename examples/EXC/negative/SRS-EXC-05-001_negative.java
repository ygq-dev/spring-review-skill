import org.springframework.scheduling.annotation.Async;

class SRS_EXC_05_001_negative {
    @Async
    public void run() {
        doWork();
    }

    private void doWork() {
        throw new IllegalStateException("bad");
    }
}