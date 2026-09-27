import java.sql.Connection;
import java.sql.Statement;
import org.springframework.stereotype.Service;

@Service
public class SqlConcatService {
    private final Connection connection;

    public SqlConcatService(Connection connection) {
        this.connection = connection;
    }

    public void findUser(String userName) throws Exception {
        String sql = "SELECT * FROM users WHERE name = '" + userName + "'";
        Statement statement = connection.createStatement();
        statement.executeQuery(sql);
    }
}