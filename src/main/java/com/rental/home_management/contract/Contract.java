package com.rental.home_management.contract;

import com.rental.home_management.common.BaseEntity;
import com.rental.home_management.room.Room;
import com.rental.home_management.tenant.Tenant;
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
@Table(name = "contracts")
public class Contract extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tenant_id", nullable = false)
  private Tenant tenant;

  @Column(nullable = false)
  private LocalDate startDate;

  @Column() private LocalDate endDate;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal monthlyRent;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal depositAmount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private ContractStatus status;

  @Column(nullable = false)
  private int paymentDueDay;

  @Column(length = 1000)
  private String note;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal electricityPrice;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private com.rental.home_management.water.WaterCalculationType waterCalculationType;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal waterPrice;
}
