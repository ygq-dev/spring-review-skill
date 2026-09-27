import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

@Service
public class SafeDeserializeService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SafeDto read(String json) throws Exception {
        return objectMapper.readValue(json, SafeDto.class);
    }

    public static class SafeDto {
        public String name;
    }
}