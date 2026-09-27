package evaluation.defects;

import org.springframework.beans.factory.annotation.Autowired;

public class DEFECT_002 {
    @Autowired
    private Dependency dependency;

    interface Dependency {
    }
}
