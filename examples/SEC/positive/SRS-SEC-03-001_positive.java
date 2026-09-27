import java.sql.Connection;
import java.sql.PreparedStatement;
import org.springframework.stereotype.Service;

@Service
public class SafeSqlService {
    private final Connection connection;

    public SafeSqlService(Connection connection) {
        this.connection = connection;
    }

    public void findUser(String userName) throws Exception {
        String sql = "SELECT * FROM users WHERE name = ?";
        PreparedStatement statement = connection.prepareStatement(sql);
        statement.setString(1, userName);
        statement.executeQuery();
    }
}