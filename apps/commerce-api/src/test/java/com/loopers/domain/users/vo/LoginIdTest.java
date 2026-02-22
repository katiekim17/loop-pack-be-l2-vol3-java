package com.loopers.domain.users.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LoginIdTest {

    @DisplayName("로그인 아이디를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("영문자와 숫자로만 이루어진 경우, 정상적으로 생성된다.")
        @Test
        void createsLoginId_whenValueIsAlphanumeric() {
            // arrange & act
            LoginId loginId = new LoginId("testuser123");

            // assert
            assertThat(loginId.value()).isEqualTo("testuser123");
        }

        @DisplayName("null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new LoginId(null)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsBlank() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new LoginId("   ")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("영문자·숫자 외의 문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueContainsSpecialChars() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new LoginId("user!@#")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}