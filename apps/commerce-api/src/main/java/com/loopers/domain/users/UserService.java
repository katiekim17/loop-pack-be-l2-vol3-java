package com.loopers.domain.users;

import com.loopers.domain.users.vo.Email;
import com.loopers.domain.users.vo.EncryptedPassword;
import com.loopers.domain.users.vo.LoginId;
import com.loopers.domain.users.vo.RawPassword;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


// 유즈케이스 흐름을 책임지도록!, Facade에서 엔티티를 만들지 않게 하려면, 서비스가 입력값을 받아서 엔티티를 생성
@RequiredArgsConstructor
@Component
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Users register(String loginId, String rawPassword, String name, String birthDate, String email) {

        // 1) 중복 체크 (최종 방어는 DB unique constraint)
        if (userRepository.existsByLoginId(loginId)) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 아이디입니다.");
        }

        // 2) VO 생성 + 검증 (Service가 담당)
        LoginId lid = LoginId.of(loginId);
        Email em = Email.of(email);
        RawPassword rp = RawPassword.of(rawPassword, birthDate);
        EncryptedPassword ep = EncryptedPassword.of(passwordEncoder.encode(rp.value()));

        // 3) 엔티티 생성 — 이미 준비된 VO만 전달
        Users users = Users.create(lid, ep, name, birthDate, em);

        // 4) 저장 + 최종 중복 방어
        try {
            return userRepository.save(users);
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.CONFLICT, "이미 존재하는 아이디입니다.");
        }
    }

    @Transactional(readOnly = true)
    public Users authenticate(String loginId, String password) {
        Users users = getMember(loginId);

        if (!passwordEncoder.matches(password, users.getPassword())) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "인증 실패");
        }

        return users;
    }

    @Transactional(readOnly = true)
    public Users getMember(String loginId) {
        return userRepository.findByLoginId(loginId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND,
                "[id = " + loginId + "] 회원을 찾을 수 없습니다."));
    }

    @Transactional
    public void changePassword(String loginId, String prevPassword, String newPassword) {
        Users users = getMember(loginId);

        // 입력한 기존 비밀번호 vs DB 암호화 값 비교
        if (!passwordEncoder.matches(prevPassword, users.getPassword())) {
            throw new CoreException(ErrorType.UNAUTHORIZED, "기존 비밀번호가 일치하지 않습니다.");
        }

        // 새 비밀번호 = 기존 비밀번호 중복 방지
        if (passwordEncoder.matches(newPassword, users.getPassword())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "새 비밀번호는 기존 비밀번호와 달라야 합니다.");
        }

        // VO 검증 + 암호화 + Dirty Checking으로 자동 저장
        users.changePassword(newPassword, raw -> passwordEncoder.encode(raw));
    }
}