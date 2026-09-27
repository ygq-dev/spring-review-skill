import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfInvocationService {
    public void outer() {
        this.inner();
    }

    @Transactional
    public void inner() {
        // ...
    }
}