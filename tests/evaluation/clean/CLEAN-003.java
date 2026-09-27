package evaluation.clean;

import org.springframework.transaction.annotation.Transactional;

public class CLEAN_003 {
    @Transactional(readOnly = true)
    public String findUser(long id) {
        return "user-" + id;
    }
}
