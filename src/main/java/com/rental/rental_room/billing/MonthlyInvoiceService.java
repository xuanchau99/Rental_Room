package com.rental.rental_room.billing;

import com.rental.rental_room.api.Requests;
import com.rental.rental_room.common.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MonthlyInvoiceService {
  private final InvoiceService invoices;
  private final BillingPolicy policy;

  public InvoiceService.MonthlyResult generate(Requests.Monthly input) {
    var period = policy.period(input.month(), input.year());
    var created = new ArrayList<Long>();
    var existing = new ArrayList<Long>();
    var failed = new LinkedHashMap<Long, String>();
    for (var room : invoices.billableRooms(input.propertyId(), period)) {
      var present = invoices.existingId(room.getId(), period);
      if (present.isPresent()) {
        existing.add(room.getId());
        continue;
      }
      try {
        invoices.generate(
            new Requests.Generate(
                room.getId(), input.month(), input.year(), BigDecimal.ZERO, BigDecimal.ZERO));
        created.add(room.getId());
      } catch (BusinessException ex) {
        failed.put(room.getId(), ex.getMessage());
      }
    }
    return new InvoiceService.MonthlyResult(created, existing, failed);
  }
}
