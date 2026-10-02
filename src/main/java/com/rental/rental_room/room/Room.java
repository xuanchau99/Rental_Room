package com.rental.rental_room.room;

import com.rental.rental_room.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.rental.rental_room.property.Property;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "rooms", uniqueConstraints = @UniqueConstraint(columnNames = {"property_id", "code"}))
public class Room extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, length = 30)
    private String code;

    @Column(name = "area_m2", nullable = false, precision = 8, scale = 2)
    private BigDecimal areaM2;

    @Column(nullable = false)
    private boolean hasMezzanine;

    @Column(nullable = false)
    private boolean hasAirConditioner;

    @Column(precision = 14, scale = 2)
    private BigDecimal monthlyRent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomStatus status;
    @Column(length = 150) private String name;
    @Column(precision = 14, scale = 2) private BigDecimal depositAmount;
    @Column(length = 1000) private String description;
}
