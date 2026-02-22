package com.loopers.infrastructure.users;

import com.loopers.domain.users.UserRepository;
import com.loopers.domain.users.Users;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository userJpaRepository;

    @Override
    public Users save(Users users) {
        return userJpaRepository.save(users);
    }

    @Override
    public Optional<Users> findByLoginId(String loginId) {
        return userJpaRepository.findByLoginIdValue(loginId);
    }

    @Override
    public boolean existsByLoginId(String loginId) {
        return userJpaRepository.findByLoginIdValue(loginId).isPresent();
    }
}