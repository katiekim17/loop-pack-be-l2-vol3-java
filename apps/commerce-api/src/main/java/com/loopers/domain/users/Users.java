package com.loopers.domain.users;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.users.vo.Email;
import com.loopers.domain.users.vo.EncryptedPassword;
import com.loopers.domain.users.vo.LoginId;
import com.loopers.domain.users.vo.RawPassword;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.function.Function;

@Entity
@Table(
    name = "users",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_login_id", columnNames = "login_id"),
        @UniqueConstraint(name = "uk_users_email", columnNames = "email")
    }
)
public class Users extends BaseEntity {

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "login_id", nullable = false, length = 50))
    private LoginId loginId;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "password", nullable = false, length = 255))
    private EncryptedPassword password;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "birth_date", nullable = false, length = 8)
    private String birthDate;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, length = 255))
    private Email email;

    protected Users() {}

    private Users(LoginId loginId, EncryptedPassword password, String name, String birthDate, Email email) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.");
        }
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
        this.loginId = loginId;
        this.password = password;
        this.name = name;
        this.birthDate = birthDate;
        this.email = email;
    }

    /**
     * 회원가입 팩토리 메서드
     * - 이미 검증·암호화된 VO를 받아서 엔티티를 생성
     * - 검증(RawPassword)과 암호화는 Service에서 담당
     */
    public static Users create(LoginId loginId, EncryptedPassword password, String name, String birthDate, Email email) {
        return new Users(loginId, password, name, birthDate, email);
    }

    public void changePassword(String newRawPassword, Function<String, String> encoder) {
        RawPassword rp = RawPassword.of(newRawPassword, this.birthDate);
        this.password = EncryptedPassword.of(encoder.apply(rp.value()));
    }

    public String getLoginId() {
        return loginId.value();
    }

    public String getPassword() {
        return password.value();
    }

    public String getName() {
        return name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getEmail() {
        return email.value();
    }

    public String getMaskedName() {
        if (name.length() == 1) {
            return "*";
        }
        return name.substring(0, name.length() - 1) + "*";
    }
}