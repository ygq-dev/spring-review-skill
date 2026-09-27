import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorrectRollbackService {
    @Transactional(rollbackFor = BusinessException.class)
    public void execute() throws BusinessException {
        throw new BusinessException("business error");
    }

    static class BusinessException extends Exception {
        BusinessException(String message) {
            super(message);
        }
    }
}