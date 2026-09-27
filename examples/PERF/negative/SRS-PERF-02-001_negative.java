package com.example.perf;

import java.util.List;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {
    List<User> findAll();
}

class User {}