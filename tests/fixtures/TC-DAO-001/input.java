import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrivateTransactionalService {
    @Transactional
    private void createOrder() {
        // ...
    }
}