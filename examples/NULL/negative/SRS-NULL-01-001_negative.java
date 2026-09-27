package com.example.nullcheck;

import java.util.Optional;

public class UserService {
    private Optional<String> userName;

    public void setUserName(Optional<String> userName) {
        this.userName = userName;
    }
}