package com.rental.rental_room.service;
import com.rental.rental_room.common.BaseEntity;
import com.rental.rental_room.contract.Contract;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
@Getter @Setter @NoArgsConstructor
@Entity @Table(name="service_usages",uniqueConstraints=@UniqueConstraint(columnNames={"contract_id","service_id","billing_year","billing_month"}))
public class ServiceUsage extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="contract_id",nullable=false) private Contract contract;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_id",nullable=false) private RentalService service;
    @Column(name="billing_month",nullable=false) private int month;
    @Column(name="billing_year",nullable=false) private int year;
    @Column(nullable=false,precision=14,scale=3) private BigDecimal quantity;
}
