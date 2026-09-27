import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class UserService {
    private final TransactionTemplate transactionTemplate;

    public UserService(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    public void asyncUpdate(Runnable task) {
        new Thread(() -> transactionTemplate.execute(status -> {
            task.run();
            return null;
        })).start();
    }
}