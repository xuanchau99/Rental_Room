package com.rental.rental_room.electricity;
import com.rental.rental_room.common.BaseEntity;
import com.rental.rental_room.room.Room;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "electricity_meters", uniqueConstraints = @UniqueConstraint(columnNames = {"room_id","billing_year","billing_month"}))
public class ElectricityMeter extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false) private Room room;
    @Column(nullable = false) private LocalDate readingDate;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal reading;
    @Column(name = "billing_month", nullable = false) private int month;
    @Column(name = "billing_year", nullable = false) private int year;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal previousReading;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal currentReading;
    @Column(nullable = false, precision = 14, scale = 3) private BigDecimal usage;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal unitPrice;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(length = 1000) private String imageUrl;
}
