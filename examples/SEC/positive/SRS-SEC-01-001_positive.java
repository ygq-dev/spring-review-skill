import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SecretConfigService {
    private final String password;

    public SecretConfigService(@Value("${app.password}") String password) {
        this.password = password;
    }

    public String loadSecret() {
        return password;
    }
}