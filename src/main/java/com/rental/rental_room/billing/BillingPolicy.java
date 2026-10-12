package com.rental.rental_room.billing;

import com.rental.rental_room.common.*;
import com.rental.rental_room.contract.*;
import com.rental.rental_room.occupancy.*;
import java.math.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BillingPolicy {
  private final Database db;

  public Contract contract(Long roomId, YearMonth period) {
    var found =
        db.query(
            "select c from Contract c where c.room.id=:room and c.status<>:pending and c.startDate<=:last and (c.endDate is null or c.endDate>=:first)",
            Contract.class,
            "room",
            roomId,
            "pending",
            ContractStatus.PENDING,
            "last",
            period.atEndOfMonth(),
            "first",
            period.atDay(1));
    if (found.isEmpty()) throw BusinessException.conflict("No contract for this room and period");
    if (found.size() != 1)
      throw BusinessException.conflict(
          "Multiple contracts in this month require a manual settlement; monthly invoice generation is blocked");
    return found.getFirst();
  }

  public YearMonth period(int month, int year) {
    var p = Dates.period(month, year);
    if (p.isAfter(YearMonth.from(Dates.today())))
      throw BusinessException.invalid("Future billing period is not allowed");
    return p;
  }

  public int people(Long contractId, YearMonth period) {
    return db.query(
            "select count(o) from RoomTenant o where o.contract.id=:id and o.moveInDate<=:last and (o.moveOutDate is null or o.moveOutDate>=:first)",
            Long.class,
            "id",
            contractId,
            "last",
            period.atEndOfMonth(),
            "first",
            period.atDay(1))
        .getFirst()
        .intValue();
  }

  public BigDecimal rent(Contract c, YearMonth p) {
    LocalDate first = c.getStartDate().isAfter(p.atDay(1)) ? c.getStartDate() : p.atDay(1);
    LocalDate last =
        c.getEndDate() != null && c.getEndDate().isBefore(p.atEndOfMonth())
            ? c.getEndDate()
            : p.atEndOfMonth();
    long days = java.time.temporal.ChronoUnit.DAYS.between(first, last) + 1;
    return c.getMonthlyRent()
        .multiply(BigDecimal.valueOf(days))
        .divide(BigDecimal.valueOf(p.lengthOfMonth()), 0, RoundingMode.CEILING);
  }
}
