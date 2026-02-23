package com.loopers.domain.common;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Money {

    @Column(name = "value", nullable = false)
    private Long value;

    protected Money() {}

    public Money(Long value) {
        if (value == null || value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "금액은 0 이상이어야 합니다.");
        }
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money)) return false;
        return Objects.equals(value, ((Money) o).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}