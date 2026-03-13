package com.loopers.domain.users;

import java.util.Optional;

public interface UserRepository {
    Users save(Users users);
    Optional<Users> findByLoginId(String loginId);
    boolean existsByLoginId(String loginId);
}