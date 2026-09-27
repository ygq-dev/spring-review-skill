import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

@Service
public class RemoteInsideTransactionService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void process() {
        restTemplate.getForObject("http://example.com", String.class);
    }
}