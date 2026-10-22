package com.rental.home_management.occupancy;

import com.rental.home_management.common.BaseEntity;
import com.rental.home_management.contract.Contract;
import com.rental.home_management.room.Room;
import com.rental.home_management.tenant.Tenant;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "room_tenants")
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

  @Column() private LocalDate moveOutDate;
}
