package com.rental.home_management.common;

import java.time.*;

public final class Dates {
  private Dates() {}

  public static LocalDate today() {
    return LocalDate.now(ZoneId.of("Asia/Bangkok"));
  }

  public static YearMonth period(int month, int year) {
    if (month < 1 || month > 12 || year < 2000 || year > 9999)
      throw BusinessException.invalid("Invalid month/year");
    return YearMonth.of(year, month);
  }
}
