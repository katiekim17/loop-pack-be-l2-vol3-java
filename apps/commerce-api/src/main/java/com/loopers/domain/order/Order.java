package com.loopers.domain.order;

import com.loopers.domain.common.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;

public class Order {

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "total_amount"))
  private Money totalAmount;
}
