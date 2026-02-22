package com.loopers.domain.users.vo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailTest {

    @DisplayName("이메일을 생성할 때, ")
    @Nested
    class Create {

        @DisplayName("올바른 이메일 형식이면, 정상적으로 생성된다.")
        @Test
        void createsEmail_whenValueIsValid() {
            // arrange & act
            Email email = new Email("test@example.com");

            // assert
            assertThat(email.value()).isEqualTo("test@example.com");
        }

        @DisplayName("null이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsNull() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Email(null)
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("빈 문자열이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueIsBlank() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Email("   ")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("@ 문자가 없으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequest_whenValueHasNoAtSign() {
            // arrange & act
            CoreException result = assertThrows(CoreException.class, () ->
                new Email("testexample.com")
            );

            // assert
            assertThat(result.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }
}