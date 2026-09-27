class Input {
    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            // ignore
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}