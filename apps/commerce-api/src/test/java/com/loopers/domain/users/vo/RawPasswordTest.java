package com.loopers.domain.users.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RawPasswordTest {

    @DisplayName("비밀번호를 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("규칙을 모두 만족하면, 정상적으로 생성된다.")
        @Test
        void createsRawPassword_whenValueIsValid() {
            // arrange & act
            RawPassword password = RawPassword.of("Test1234!", "19900101");

            // assert
            assertThat(password.value()).isEqualTo("Test1234!");
        }

        @DisplayName("null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                RawPassword.of(null, "19900101")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("7자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsTooShort() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                RawPassword.of("Test12!", "19900101")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("17자 초과이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsTooLong() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                RawPassword.of("Test123456789012!", "19900101")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("한글이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueContainsKorean() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                RawPassword.of("Test홍길동!", "19900101")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("생년월일이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueContainsBirthDate() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                RawPassword.of("Test19900101!", "19900101")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}