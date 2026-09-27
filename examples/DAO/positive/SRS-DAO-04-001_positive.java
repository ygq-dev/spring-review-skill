import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class RemoteOutsideTransactionService {
    private final RestTemplate restTemplate = new RestTemplate();

    public void process() {
        String result = restTemplate.getForObject("http://example.com", String.class);
        save(result);
    }

    @Transactional
    public void save(String value) {
        // ...
    }
}