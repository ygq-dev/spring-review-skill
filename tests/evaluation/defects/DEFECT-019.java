package evaluation.defects;

import javax.servlet.http.HttpServletRequest;

public class DEFECT_019 {
    public String extract(HttpServletRequest request) {
        return request.getHeader("X-User");
    }
}
