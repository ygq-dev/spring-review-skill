package evaluation.clean;

import org.springframework.context.MessageSource;

public class CLEAN_010 {
    private final MessageSource messageSource;

    public CLEAN_010(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String login(boolean ok) {
        return messageSource.getMessage(ok ? "login.ok" : "login.fail", null, null);
    }
}
