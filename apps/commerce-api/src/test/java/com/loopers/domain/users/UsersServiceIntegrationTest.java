package com.loopers.domain.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class UsersServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원가입을 할 때, ")
    @Nested
    class SaveUsers {

        @DisplayName("필수 정보가 모두 주어지면, DB에 저장되고 조회할 수 있다.")
        @Test
        void returnsUserInfo_whenValidUserInfoIsProvided() {
            // arrange
            String loginId = "testuser";
            String password = "Test1234!";
            String name = "홍길동";
            String birthDate = "19900101";
            String email = "test@example.com";

            // act
            userService.register(loginId, password, name, birthDate, email);
            Users result = userService.getMember(loginId);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getLoginId()).isEqualTo(loginId),
                () -> assertThat(result.getName()).isEqualTo(name),
                () -> assertThat(result.getBirthDate()).isEqualTo(birthDate),
                () -> assertThat(result.getEmail()).isEqualTo(email)
            );
        }

        @DisplayName("중복 ID로 가입 시도하면, CONFLICT 예외가 발생한다.")
        @Test
        void throwsException_whenExistIdIsTryToSaveUser() {
            // arrange — 먼저 한 명 가입시켜 DB에 저장
            String loginId = "testuser";
            userService.register(loginId, "Test1234!", "홍길동", "19900101", "test@example.com");

            // act — 같은 ID로 또 가입 시도
            CoreException exception = assertThrows(CoreException.class, () ->
                userService.register(loginId, "Test1234!", "홍길동", "19900101", "test@example.com")
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.CONFLICT);
        }
    }

    @DisplayName("회원을 조회할 때, ")
    @Nested
    class GetUsers {

        @DisplayName("존재하는 ID를 주면, 해당 유저 정보를 반환한다.")
        @Test
        void returnsUserInfo_whenValidIdIsProvided() {
            // arrange
            String loginId = "testuser";
            String password = "Test1234!";
            String name = "홍길동";
            String birthDate = "19900101";
            String email = "test@example.com";
            userService.register(loginId, password, name, birthDate, email);

            // act
            Users result = userService.getMember(loginId);

            // assert
            assertAll(
                () -> assertThat(result).isNotNull(),
                () -> assertThat(result.getLoginId()).isEqualTo(loginId),
                () -> assertThat(result.getName()).isEqualTo(name),
                () -> assertThat(result.getBirthDate()).isEqualTo(birthDate),
                () -> assertThat(result.getEmail()).isEqualTo(email)
            );
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsException_whenMemberNotFound() {
            // arrange
            String loginId = "nonexistent";

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                userService.getMember(loginId)
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("비밀번호 변경을 할 때, ")
    @Nested
    class ChangePassword {

        @DisplayName("기존 비밀번호와 새 비밀번호가 유효하면, 비밀번호가 변경된다.")
        @Test
        void changesPassword_whenOldAndNewPasswordsAreValid() {
            // arrange
            String loginId = "testuser";
            String prevPassword = "Test1234!";
            String newPassword = "NewPass123!";
            userService.register(loginId, prevPassword, "홍길동", "19900101", "test@test.co.kr");
            String beforeEncrypted = userService.getMember(loginId).getPassword();

            // act
            userService.changePassword(loginId, prevPassword, newPassword);

            // assert
            Users after = userService.getMember(loginId);
            assertThat(after.getPassword()).isNotEqualTo(beforeEncrypted);
        }

        @DisplayName("기존 비밀번호가 일치하지 않으면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        void throwsException_whenOldPasswordDoesNotMatch() {
            // arrange
            String loginId = "testuser";
            userService.register(loginId, "Test1234!", "홍길동", "19900101", "test@test.co.kr");

            // act — 틀린 비밀번호로 시도
            CoreException exception = assertThrows(CoreException.class, () ->
                userService.changePassword(loginId, "WrongPass123!", "NewPass456!")
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.UNAUTHORIZED);
        }

        @DisplayName("새 비밀번호가 기존 비밀번호와 같으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenNewPasswordIsSameAsOld() {
            // arrange
            String loginId = "testuser";
            String prevPassword = "Test1234!";
            userService.register(loginId, prevPassword, "홍길동", "19900101", "test@test.co.kr");

            // act
            CoreException exception = assertThrows(CoreException.class, () ->
                userService.changePassword(loginId, prevPassword, prevPassword)
            );

            // assert
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}