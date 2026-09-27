import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LoopQueryService {
    private final UserRepository userRepository;

    public LoopQueryService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> findUsers(List<Long> ids) {
        List<User> users = new ArrayList<>();
        for (Long id : ids) {
            users.add(userRepository.findById(id).orElse(null));
        }
        return users;
    }

    interface UserRepository {
        java.util.Optional<User> findById(Long id);
    }

    static class User {
    }
}