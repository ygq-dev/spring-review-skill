import java.sql.Connection;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UnsafeResourceService {
    private final DataSource dataSource;

    public UnsafeResourceService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Transactional
    public void query() throws Exception {
        Connection connection = dataSource.getConnection();
        connection.createStatement().executeQuery("SELECT 1");
    }
}