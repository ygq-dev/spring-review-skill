package evaluation.clean;

import org.springframework.stereotype.Service;

@Service
public class CLEAN_001 {
    private final Dependency dependency;

    public CLEAN_001(Dependency dependency) {
        this.dependency = dependency;
    }

    interface Dependency {
    }
}
