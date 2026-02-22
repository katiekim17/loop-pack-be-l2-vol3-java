package com.loopers.infrastructure.users;

import com.loopers.domain.users.Users;
import com.loopers.domain.users.vo.LoginId;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<Users, Long> {

    // loginId 필드는 @Embedded LoginId 타입 → "loginId.value" 경로로 조회
    Optional<Users> findByLoginIdValue(String value);
}