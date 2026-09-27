import org.springframework.stereotype.Service;

@Service
public class HardcodedSecretService {
    private final String password = "admin123";
    private final String apiKey = "sk-xxx";

    public String loadSecret() {
        return password + apiKey;
    }
}