package com.rental.home_management.deposit;

import com.rental.home_management.common.BaseEntity;
import com.rental.home_management.contract.Contract;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "deposit_transactions")
public class DepositTransaction extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contract_id", nullable = false)
  private Contract contract;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private DepositType type;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Column(nullable = false)
  private LocalDate transactionDate;

  @Column(length = 1000)
  private String note;
}
