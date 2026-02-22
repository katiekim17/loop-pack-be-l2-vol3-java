package com.loopers.domain.users.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Email {

  private String value;

  protected Email() {}

  public Email(String value) {
    if (value == null || value.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
    }
    // 필요시 더 강한 정규식/라이브러리로 교체
    if (!value.contains("@")) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
    }
    this.value = value;
  }

  public static Email of(String value) {
    return new Email(value);
  }

  public String value() {
    return value;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Email)) return false;
    return Objects.equals(value, ((Email) o).value);
  }

  @Override public int hashCode() {
    return Objects.hash(value);
  }

  @Override public String toString() {
    return value;
  }
}
