import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MissingReadOnlyService {
    @Transactional
    public String findName(long id) {
        return "name";
    }
}