package com.rental.home_management.billing;

import com.rental.home_management.api.Requests;
import com.rental.home_management.api.Views.InvoiceView;
import com.rental.home_management.api.Views.PaymentView;
import com.rental.home_management.common.BusinessException;
import com.rental.home_management.common.Database;
import com.rental.home_management.common.Dates;
import com.rental.home_management.invoice.Invoice;
import com.rental.home_management.payment.Payment;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class InvoicePaymentService {
  private final Database db;
  private final InvoiceQueryService queries;

  public PaymentView pay(Long invoiceId, Requests.Pay request) {
    var invoice = db.lock(Invoice.class, invoiceId);
    var existingPayments =
        db.query(
            "select p from Payment p where p.invoice.id=:invoice",
            Payment.class,
            "invoice",
            invoiceId);
    var paid =
        existingPayments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    var cumulative = paid.add(request.amount());
    if (cumulative.compareTo(invoice.getTotalAmount()) > 0) {
      throw BusinessException.conflict("Payment exceeds the invoice outstanding balance");
    }
    try {
      invoice.registerPayment(cumulative, request.paymentDate(), Dates.today());
    } catch (IllegalStateException | IllegalArgumentException ex) {
      throw BusinessException.conflict(ex.getMessage());
    }
    var payment = new Payment();
    payment.setInvoice(invoice);
    payment.setAmount(request.amount());
    payment.setMethod(request.paymentMethod());
    payment.setPaidAt(request.paymentDate());
    payment.setReference(request.referenceCode());
    payment.setNote(request.note());
    db.save(payment);
    db.em().flush();
    return PaymentView.of(payment);
  }

  @Transactional(readOnly = true)
  public List<PaymentView> payments(Long invoiceId) {
    db.get(Invoice.class, invoiceId);
    return db
        .query(
            "select p from Payment p where p.invoice.id=:invoice order by p.paidAt, p.id",
            Payment.class,
            "invoice",
            invoiceId)
        .stream()
        .map(PaymentView::of)
        .toList();
  }

  public InvoiceView cancel(Long invoiceId) {
    var invoice = db.lock(Invoice.class, invoiceId);
    try {
      invoice.cancel();
    } catch (IllegalStateException ex) {
      throw BusinessException.conflict(ex.getMessage());
    }
    db.em().flush();
    return queries.get(invoiceId);
  }
}
