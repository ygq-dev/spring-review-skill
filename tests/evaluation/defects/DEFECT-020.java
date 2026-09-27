package evaluation.defects;

public class DEFECT_020 {
    public String login(boolean ok) {
        if (ok) {
            return "登录成功";
        }
        return "登录失败，请重试";
    }
}
