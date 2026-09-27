package evaluation.defects;

import org.springframework.transaction.annotation.Transactional;

public class DEFECT_003 {
    @Transactional
    private void badTransaction() {
    }
}
