class SRS_EXC_02_001_negative {
    void handle() {
        try {
            doWork();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doWork() {
        throw new IllegalArgumentException("bad");
    }
}