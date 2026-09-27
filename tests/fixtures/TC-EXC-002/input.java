class Input {
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