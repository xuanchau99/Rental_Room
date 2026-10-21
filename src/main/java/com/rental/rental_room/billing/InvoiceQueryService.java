package com.rental.rental_room.billing;

import com.rental.rental_room.api.Views.InvoiceView;
import com.rental.rental_room.api.Views.ItemView;
import com.rental.rental_room.common.BusinessException;
import com.rental.rental_room.common.Database;
import com.rental.rental_room.common.Dates;
import com.rental.rental_room.invoice.Invoice;
import com.rental.rental_room.invoice.InvoiceItem;
import com.rental.rental_room.invoice.InvoiceStatus;
import com.rental.rental_room.payment.Payment;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceQueryService {
  private final Database db;

  public InvoiceView get(Long id) {
    return view(db.get(Invoice.class, id));
  }

  public List<InvoiceView> list(
      Long roomId, Integer month, Integer year, InvoiceStatus status, int page, int size) {
    if (page < 0 || size < 1 || size > 100) {
      throw BusinessException.invalid("page >= 0 and size 1..100 required");
    }
    var invoices =
        db.query(
            "select i from Invoice i where (:room is null or i.room.id=:room) "
                + "and (:month is null or i.month=:month) "
                + "and (:year is null or i.year=:year) order by i.year desc, i.month desc, i.id desc",
            Invoice.class,
            "room",
            roomId,
            "month",
            month,
            "year",
            year);
    var matching =
        invoices.stream()
            .map(this::view)
            .filter(v -> status == null || v.status() == status)
            .toList();
    int from = Math.min(Math.multiplyExact(page, size), matching.size());
    int to = Math.min(from + size, matching.size());
    return matching.subList(from, to);
  }

  private InvoiceView view(Invoice invoice) {
    var payments =
        db.query(
            "select p from Payment p where p.invoice.id=:invoice order by p.paidAt, p.id",
            Payment.class,
            "invoice",
            invoice.getId());
    var paid = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var items =
        db
            .query(
                "select i from InvoiceItem i where i.invoice.id=:invoice order by i.id",
                InvoiceItem.class,
                "invoice",
                invoice.getId())
            .stream()
            .map(ItemView::of)
            .toList();
    var effectiveStatus = invoice.effectiveStatus(paid, Dates.today());
    return new InvoiceView(
        invoice.getId(),
        invoice.getRoom().getId(),
        invoice.getContract().getId(),
        invoice.getNumber(),
        invoice.getMonth(),
        invoice.getYear(),
        invoice.getRoomAmount(),
        invoice.getElectricAmount(),
        invoice.getWaterAmount(),
        invoice.getServiceAmount(),
        invoice.getOtherAmount(),
        invoice.getDiscount(),
        invoice.getTotalAmount(),
        paid,
        invoice.getTotalAmount().subtract(paid).max(BigDecimal.ZERO),
        effectiveStatus,
        invoice.getDueDate(),
        invoice.getPaidAt(),
        items);
  }
}
