package evaluation.defects;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

public class DEFECT_006 {
    private final RestTemplate restTemplate = new RestTemplate();

    @Transactional
    public void process() {
        restTemplate.getForObject("https://example.com", String.class);
    }
}
