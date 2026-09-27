package evaluation.defects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DEFECT_001 {
    @Autowired
    private Dependency dependency;

    public String run() {
        return dependency != null ? "ok" : "no";
    }

    interface Dependency {
    }
}
