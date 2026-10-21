package com.rental.rental_room.billing;

import com.rental.rental_room.common.Database;
import com.rental.rental_room.common.Dates;
import com.rental.rental_room.invoice.Invoice;
import com.rental.rental_room.payment.Payment;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceExportDataService {
  private final Database db;

  public record ExportRow(
      String number,
      String roomCode,
      int month,
      int year,
      BigDecimal roomAmount,
      BigDecimal electricAmount,
      BigDecimal waterAmount,
      BigDecimal serviceAmount,
      BigDecimal otherAmount,
      BigDecimal discount,
      BigDecimal totalAmount,
      BigDecimal paidAmount,
      BigDecimal outstandingAmount,
      String status) {}

  public List<ExportRow> rows(int month, int year, Long propertyId) {
    var invoices =
        db.query(
            "select i from Invoice i where i.month=:month and i.year=:year "
                + "and (:propertyId is null or i.room.property.id=:propertyId) order by i.room.id",
            Invoice.class,
            "month",
            month,
            "year",
            year,
            "propertyId",
            propertyId);
    return invoices.stream().map(this::row).toList();
  }

  private ExportRow row(Invoice invoice) {
    var payments =
        db.query(
            "select p from Payment p where p.invoice.id=:invoice",
            Payment.class,
            "invoice",
            invoice.getId());
    var paid =
        payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ExportRow(
        invoice.getNumber(),
        invoice.getRoom().getCode(),
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
        invoice.effectiveStatus(paid, Dates.today()).name());
  }
}
