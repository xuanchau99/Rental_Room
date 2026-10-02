package com.rental.rental_room.service;
import com.rental.rental_room.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.rental.rental_room.contract.Contract;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "contract_services")
public class ContractService extends BaseEntity {
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
