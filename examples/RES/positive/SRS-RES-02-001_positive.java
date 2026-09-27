import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SafeResourceService {
    private final DataSource dataSource;

    public SafeResourceService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Transactional
    public void query() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            resultSet.next();
        }
    }
}