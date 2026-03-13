package com.loopers.domain.users.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import java.util.Objects;

// VO는 도메인 규칙(형식/제약)을 “자기 책임”으로 갖게 되고,
@Embeddable
public class LoginId {

  private String value;

  protected LoginId() {}

  public LoginId(String value) {
    if (value == null || value.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "아이디는 비어있을 수 없습니다.");
    }
    if (!value.matches("^[a-zA-Z0-9]+$")) {
      throw new CoreException(ErrorType.BAD_REQUEST, "로그인 아이디는 영문자와 숫자만 사용할 수 있습니다.");
    }
    this.value = value;
  }

  public static LoginId of(String value) {
    return new LoginId(value);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LoginId)) {
      return false;
    }
    return Objects.equals(value, ((LoginId) o).value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }

  @Override
  public String toString() {
    return value;
  }
}

