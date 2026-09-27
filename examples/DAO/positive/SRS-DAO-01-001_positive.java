import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicTransactionalService {
    @Transactional
    public void createOrder() {
        // ...
    }
}