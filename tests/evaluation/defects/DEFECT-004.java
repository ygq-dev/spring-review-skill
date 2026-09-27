package evaluation.defects;

import org.springframework.transaction.annotation.Transactional;

public class DEFECT_004 {
    public void outer() {
        this.inner();
    }

    @Transactional
    public void inner() {
    }
}
