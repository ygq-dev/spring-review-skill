class SRS_EXC_01_001_negative {
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