package evaluation.defects;

public class DEFECT_016 {
    public void run() {
        try {
            doWork();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doWork() {
    }
}
