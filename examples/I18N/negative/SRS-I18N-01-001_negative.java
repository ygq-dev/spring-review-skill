package com.example.i18n;

public class LoginController {
    public String login(String username, String password) {
        if (username == null || password == null) {
            return "登录失败，请重试";
        }
        return "登录成功";
    }
}