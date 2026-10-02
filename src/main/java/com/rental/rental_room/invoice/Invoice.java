package com.rental.rental_room.invoice;

import com.rental.rental_room.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.rental.rental_room.contract.Contract;
import java.time.LocalDate;
import java.math.BigDecimal;

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
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceStatus status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false) private com.rental.rental_room.room.Room room;
    @Column(name = "billing_month", nullable = false) private int month;
    @Column(name = "billing_year", nullable = false) private int year;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal roomAmount;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal electricAmount;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal waterAmount;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal serviceAmount;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal otherAmount;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal discount;
    private java.time.Instant paidAt;
}
