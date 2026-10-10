package com.rental.rental_room.water;

import com.rental.rental_room.common.BaseEntity;
import com.rental.rental_room.room.Room;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "water_meters",
    uniqueConstraints =
        @UniqueConstraint(columnNames = {"room_id", "billing_year", "billing_month"}))
public class WaterMeter extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "room_id", nullable = false)
  private Room room;

  @Column(nullable = false)
  private LocalDate readingDate;

  @Column(nullable = false, precision = 14, scale = 3)
  private BigDecimal reading;

  @Column(name = "billing_month", nullable = false)
  private int month;

  @Column(name = "billing_year", nullable = false)
  private int year;

  @Column(nullable = false, precision = 14, scale = 3)
  private BigDecimal previousReading;

  @Column(nullable = false, precision = 14, scale = 3)
  private BigDecimal currentReading;

  @Column(nullable = false, precision = 14, scale = 3)
  private BigDecimal usage;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal amount;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private WaterCalculationType calculationType;

  @Column(nullable = false)
  private int personCount;
}
