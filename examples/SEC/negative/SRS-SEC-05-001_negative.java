import java.io.InputStream;
import java.io.ObjectInputStream;
import org.springframework.stereotype.Service;

@Service
public class UnsafeDeserializeService {

    public Object read(InputStream inputStream) throws Exception {
        ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
        return objectInputStream.readObject();
    }
}