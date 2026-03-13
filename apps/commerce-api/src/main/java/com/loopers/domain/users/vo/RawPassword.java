package com.loopers.domain.users.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

public final class RawPassword {

  private final String value;

  private RawPassword(String value, String birthDate) {
    if (value == null || value.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.");
    }
    if (value.length() < 8 || value.length() > 16) {
      throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자여야 합니다.");
    }
    if (!value.matches("^[a-zA-Z0-9!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$")) {
      throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 가능합니다.");
    }
    if (birthDate != null && !birthDate.isBlank() && value.contains(birthDate)) {
      throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.");
    }
    this.value = value;
  }

  public static RawPassword of(String value, String birthDate) {
    return new RawPassword(value, birthDate);
  }

  public String value() {
    return value;
  }
}
