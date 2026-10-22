package com.rental.home_management.payment;

import com.rental.home_management.common.BaseEntity;
import com.rental.home_management.invoice.Invoice;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invoice_id", nullable = false)
  private Invoice invoice;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private Instant paidAt;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private PaymentMethod method;

  @Column(length = 100)
  private String reference;

  @Column(length = 1000)
  private String note;
}
