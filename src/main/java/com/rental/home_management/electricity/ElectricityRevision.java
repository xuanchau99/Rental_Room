package com.rental.home_management.electricity;

import com.rental.home_management.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "electricity_revisions")
public class ElectricityRevision extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reading_id", nullable = false)
  private ElectricityMeter reading;

  @Column(nullable = false, precision = 14, scale = 3)
  private BigDecimal oldReading;

  @Column(nullable = false, precision = 14, scale = 3)
  private BigDecimal newReading;

  @Column(nullable = false, length = 100)
  private String changedBy;

  @Column(nullable = false, length = 1000)
  private String reason;
}
