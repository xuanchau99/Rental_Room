package com.rental.rental_room.deposit;
import com.rental.rental_room.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.rental.rental_room.contract.Contract;
import java.math.BigDecimal;
import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "deposit_transactions")
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
