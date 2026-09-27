import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReadOnlyQueryService {
    @Transactional(readOnly = true)
    public String findName(long id) {
        return "name";
    }
}