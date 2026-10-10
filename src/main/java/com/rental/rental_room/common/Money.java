package com.rental.rental_room.common;

import java.math.*;

public final class Money {
  private Money() {}

  public static final BigDecimal ZERO = new BigDecimal("0.00");

  public static BigDecimal round(BigDecimal amount) {
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  public static BigDecimal charge(BigDecimal quantity, BigDecimal price) {
    return round(quantity.multiply(price));
  }
}
