package com.rental.home_management.invoice;

import com.rental.home_management.common.BaseEntity;
import com.rental.home_management.contract.Contract;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoice extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contract_id", nullable = false)
  private Contract contract;

  @Column(nullable = false, unique = true, length = 50)
  private String number;

  @Column(nullable = false)
  private LocalDate periodStart;

  @Column(nullable = false)
  private LocalDate periodEnd;

  @Column(nullable = false)
  private LocalDate dueDate;

  @Column(nullable = false, precision = 14, scale = 2)
  @Setter(AccessLevel.NONE)
  private BigDecimal totalAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  @Setter(AccessLevel.NONE)
  private InvoiceStatus status;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private com.rental.home_management.room.Room room;

  @Column(name = "billing_month", nullable = false)
  private int month;

  @Column(name = "billing_year", nullable = false)
  private int year;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal roomAmount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal electricAmount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal waterAmount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal serviceAmount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal otherAmount;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal discount;

  @Setter(AccessLevel.NONE)
  private Instant paidAt;

  public void initializeTotal(BigDecimal total, LocalDate today) {
    if (total.signum() < 0) throw new IllegalArgumentException("Invoice total cannot be negative");
    totalAmount = total;
    if (total.signum() == 0) {
      status = InvoiceStatus.PAID;
      paidAt = Instant.now();
    } else {
      status = dueDate.isBefore(today) ? InvoiceStatus.OVERDUE : InvoiceStatus.UNPAID;
    }
  }

  public void registerPayment(BigDecimal cumulativePaid, Instant paymentDate, LocalDate today) {
    if (status == InvoiceStatus.PAID || status == InvoiceStatus.CANCELLED)
      throw new IllegalStateException("Invoice does not accept payments in its current state");
    if (cumulativePaid.signum() <= 0 || cumulativePaid.compareTo(totalAmount) > 0)
      throw new IllegalArgumentException("Cumulative payment must be within the invoice total");
    if (cumulativePaid.compareTo(totalAmount) == 0) {
      status = InvoiceStatus.PAID;
      paidAt = paymentDate;
    } else {
      status = dueDate.isBefore(today) ? InvoiceStatus.OVERDUE : InvoiceStatus.PARTIALLY_PAID;
    }
  }

  public InvoiceStatus effectiveStatus(BigDecimal paid, LocalDate today) {
    if (status != InvoiceStatus.PAID
        && status != InvoiceStatus.CANCELLED
        && dueDate.isBefore(today)
        && totalAmount.compareTo(paid) > 0) return InvoiceStatus.OVERDUE;
    return status;
  }

  public void cancel() {
    if (status == InvoiceStatus.PAID || status == InvoiceStatus.CANCELLED)
      throw new IllegalStateException("Invoice cannot be cancelled in its current state");
    status = InvoiceStatus.CANCELLED;
  }
}
