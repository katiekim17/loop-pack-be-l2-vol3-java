package com.loopers.domain.users.vo;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import java.util.Objects;

// Password를 Raw/Encrypted로 분리하면 “평문이 엔티티/DB에 남는 실수”를 구조적으로 막을 수 있어요.
// @Embeddable을 쓰면 VO 내부 필드가 부모 테이블의 컬럼으로 펼쳐집니다. JPA가 객체를 재구성할 때 기본 생성자가 필요합니다.
@Embeddable
public class EncryptedPassword {

  private String value;

  protected EncryptedPassword() {}

  public EncryptedPassword(String value) {
    if (value == null || value.isBlank()) {
      throw new CoreException(ErrorType.BAD_REQUEST, "비밀번호는 비어있을 수 없습니다.");
    }
    this.value = value;
  }

  public static EncryptedPassword of(String value) {
    return new EncryptedPassword(value);
  }

  public String value() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof EncryptedPassword)) {
      return false;
    }
    return Objects.equals(value, ((EncryptedPassword) o).value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value);
  }
}
