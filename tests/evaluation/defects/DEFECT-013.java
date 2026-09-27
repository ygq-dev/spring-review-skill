package evaluation.defects;

public class DEFECT_013 {
    public String findUser(String name) {
        return "SELECT * FROM users WHERE name = '" + name + "'";
    }
}
