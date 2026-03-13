package com.loopers.domain.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.domain.users.vo.Email;
import com.loopers.domain.users.vo.EncryptedPassword;
import com.loopers.domain.users.vo.LoginId;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UsersTest {

    // Users.create()에 전달할 이미 검증된 VO 헬퍼
    private Users createUser(String loginId, String password, String name, String birthDate, String email) {
        return Users.create(
            new LoginId(loginId),
            new EncryptedPassword(password),
            name,
            birthDate,
            new Email(email)
        );
    }

    @DisplayName("회원을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("필수 정보가 모두 주어지면, 정상적으로 생성된다.")
        @Test
        void createsUser_whenAllFieldsAreProvided() {
            // arrange
            String loginId = "testuser";
            String password = "hashed_pw";
            String name = "홍길동";
            String birthDate = "19900101";
            String email = "test@example.com";

            // act
            Users users = createUser(loginId, password, name, birthDate, email);

            // assert
            assertAll(
                () -> assertThat(users.getLoginId()).isEqualTo(loginId),
                () -> assertThat(users.getName()).isEqualTo(name),
                () -> assertThat(users.getBirthDate()).isEqualTo(birthDate),
                () -> assertThat(users.getEmail()).isEqualTo(email)
            );
        }

        @DisplayName("이름이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                createUser("testuser", "hashed_pw", null, "19900101", "test@example.com")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이름이 빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenNameIsBlank() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                createUser("testuser", "hashed_pw", "  ", "19900101", "test@example.com")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenBirthDateIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                createUser("testuser", "hashed_pw", "홍길동", null, "test@example.com")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("이름을 마스킹할 때, ")
    @Nested
    class MaskName {

        @DisplayName("2자 이상이면 마지막 글자만 마스킹된다.")
        @Test
        void masksLastCharacter_whenNameIsMultipleChars() {
            // arrange
            Users users = createUser("testuser", "hashed_pw", "홍길동", "19900101", "test@example.com");

            // act & assert
            assertThat(users.getMaskedName()).isEqualTo("홍길*");
        }

        @DisplayName("1자이면 전체가 마스킹된다.")
        @Test
        void masksAllCharacters_whenNameIsSingleChar() {
            // arrange
            Users users = createUser("testuser", "hashed_pw", "홍", "19900101", "test@example.com");

            // act & assert
            assertThat(users.getMaskedName()).isEqualTo("*");
        }
    }
}