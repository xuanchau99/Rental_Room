package com.rental.rental_room.occupancy;
import com.rental.rental_room.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.rental.rental_room.room.Room;
import com.rental.rental_room.tenant.Tenant;
import com.rental.rental_room.contract.Contract;
import java.time.LocalDate;
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "room_tenants")
public class RoomTenant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OccupantRole role;

    @Column(nullable = false)
    private LocalDate moveInDate;

    @Column()
    private LocalDate moveOutDate;
}
