package com.example.i18n;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class LoginController {
    private final MessageSource messageSource;

    public LoginController(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    public String login(String username, String password) {
        if (username == null || password == null) {
            return messageSource.getMessage("login.failed", null, LocaleContextHolder.getLocale());
        }
        return messageSource.getMessage("login.success", null, LocaleContextHolder.getLocale());
    }
}