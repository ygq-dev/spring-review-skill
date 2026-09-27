import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissingRollbackService {
    @Transactional
    public void execute() throws Exception {
        throw new Exception("checked error");
    }
}