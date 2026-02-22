package com.loopers.domain.common;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public final class Money {
  @Column(name = "value", nullable = false)  // 엔티티별 컬럼명은 @AttributeOverrides로 재정의
  private Long value;

  private Money() {}

  public Money(Long value) {
    if (value == null || value < 0) {
      throw new CoreException(ErrorType.BAD_REQUEST, "금액은 0 이상이어야 합니다.");
    }
    this.value = value;
  }
}
