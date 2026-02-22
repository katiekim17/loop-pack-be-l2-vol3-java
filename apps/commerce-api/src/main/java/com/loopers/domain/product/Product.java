package com.loopers.domain.product;

import com.loopers.domain.common.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;

public class Product {

  @Embedded
  @AttributeOverride(name = "value", column = @Column(name = "price"))
  private Money price;
}
