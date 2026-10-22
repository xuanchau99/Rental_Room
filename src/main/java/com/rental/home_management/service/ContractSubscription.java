package com.rental.home_management.service;

import com.rental.home_management.common.BaseEntity;
import com.rental.home_management.contract.Contract;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "contract_services")
public class ContractSubscription extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "contract_id", nullable = false)
  private Contract contract;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_id", nullable = false)
  private RentalService service;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal price;

  @Column(nullable = false, precision = 14, scale = 3)
  private BigDecimal quantity;
}
