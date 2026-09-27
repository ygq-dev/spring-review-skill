import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SelfProxyService {
    @Autowired
    private SelfProxyService self;

    public void outer() {
        self.inner();
    }

    @Transactional
    public void inner() {
        // ...
    }
}