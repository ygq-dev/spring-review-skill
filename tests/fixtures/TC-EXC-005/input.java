import org.springframework.scheduling.annotation.Async;

class Input {
    @Async
    public void run() {
        doWork();
    }

    private void doWork() {
        throw new IllegalStateException("bad");
    }
}