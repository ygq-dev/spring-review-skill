import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BatchQueryService {
    private final UserRepository userRepository;

    public BatchQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findUsers(List<Long> ids) {
        return userRepository.findAllById(ids);
    }

    interface UserRepository {
        List<User> findAllById(List<Long> ids);
    }

    static class User {
    }
}