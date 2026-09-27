package evaluation.defects;

import org.springframework.transaction.annotation.Transactional;

public class DEFECT_005 {
    @Transactional
    public String findUser(long id) {
        return "user-" + id;
    }
}
