package com.rental.home_management.billing;

import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reminders")
@RequiredArgsConstructor
public class ReminderController {
  private final InvoiceService invoices;

  public record Reminder(
      Long invoiceId,
      Long roomId,
      String number,
      LocalDate dueDate,
      java.math.BigDecimal outstanding,
      String message) {}

  @GetMapping
  public List<Reminder> list(@RequestParam(defaultValue = "7") int dueWithinDays) {
    if (dueWithinDays < 0 || dueWithinDays > 60)
      throw com.rental.home_management.common.BusinessException.invalid(
          "dueWithinDays must be 0..60");
    var until = com.rental.home_management.common.Dates.today().plusDays(dueWithinDays);
    // Cursor through invoices; no customer message is sent from this endpoint.
    var result = new ArrayList<Reminder>();
    for (int page = 0; ; page++) {
      var batch = invoices.list(null, null, null, null, page, 100);
      if (batch.isEmpty()) break;
      for (var i : batch)
        if (i.outstandingAmount().signum() > 0
            && !i.status().equals(com.rental.home_management.invoice.InvoiceStatus.CANCELLED)
            && !i.dueDate().isAfter(until))
          result.add(
              new Reminder(
                  i.id(),
                  i.roomId(),
                  i.number(),
                  i.dueDate(),
                  i.outstandingAmount(),
                  "Room "
                      + i.roomId()
                      + " invoice "
                      + i.number()
                      + " due "
                      + i.dueDate()
                      + ": "
                      + i.outstandingAmount()
                      + " VND"));
      if (batch.size() < 100) break;
    }
    return result;
  }
}
