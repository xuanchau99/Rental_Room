package com.rental.rental_room.occupancy;

import com.rental.rental_room.api.Requests;
import com.rental.rental_room.api.Views.OccupantView;
import com.rental.rental_room.common.BusinessException;
import com.rental.rental_room.common.Database;
import com.rental.rental_room.common.Dates;
import com.rental.rental_room.contract.Contract;
import com.rental.rental_room.contract.ContractStatus;
import com.rental.rental_room.room.Room;
import com.rental.rental_room.tenant.Tenant;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OccupancyService {
  private final Database db;

  public RoomTenant addPrimaryStay(Contract contract, LocalDate moveInDate) {
    var stay = new RoomTenant();
    stay.setRoom(contract.getRoom());
    stay.setTenant(contract.getTenant());
    stay.setContract(contract);
    stay.setRole(OccupantRole.PRIMARY_TENANT);
    stay.setMoveInDate(moveInDate);
    return db.save(stay);
  }

  public void closeStays(Contract contract, LocalDate moveOutDate) {
    var stays = db.query(
        "select s from RoomTenant s where s.contract.id=:contract and s.moveOutDate is null",
        RoomTenant.class, "contract", contract.getId());
    for (var stay : stays) {
      if (moveOutDate.isBefore(stay.getMoveInDate()))
        throw BusinessException.invalid("Move-out date cannot be before move-in date");
      stay.setMoveOutDate(moveOutDate);
    }
  }

  @Transactional(readOnly = true)
  public List<OccupantView> occupants(Long roomId) {
    db.get(Room.class, roomId);
    return db.query(
        "select s from RoomTenant s where s.room.id=:room and s.moveOutDate is null order by s.id",
        RoomTenant.class, "room", roomId).stream().map(OccupantView::of).toList();
  }

  public OccupantView addOccupant(Long roomId, Requests.OccupantInput request) {
    var room = db.lock(Room.class, roomId);
    var contract = db.first(
        "select c from Contract c where c.room.id=:room and c.status=:status",
        Contract.class, "room", roomId, "status", ContractStatus.ACTIVE)
        .orElseThrow(() -> BusinessException.conflict("Room has no active contract"));
    if (request.moveInDate().isAfter(Dates.today()))
      throw BusinessException.invalid("Move-in date cannot be in the future");
    var tenant = db.lock(Tenant.class, request.tenantId());
    if (db.first(
        "select s from RoomTenant s where s.contract.id=:contract and s.tenant.id=:tenant and s.moveOutDate is null",
        RoomTenant.class, "contract", contract.getId(), "tenant", tenant.getId()).isPresent())
      throw BusinessException.conflict("Tenant is already an active occupant of this contract");
    var stay = new RoomTenant();
    stay.setRoom(room);
    stay.setTenant(tenant);
    stay.setContract(contract);
    stay.setRole(OccupantRole.OCCUPANT);
    stay.setMoveInDate(request.moveInDate());
    return OccupantView.of(db.save(stay));
  }

  public OccupantView leave(Long roomId, Long stayId, LocalDate moveOutDate) {
    db.lock(Room.class, roomId);
    var stay = db.lock(RoomTenant.class, stayId);
    if (!stay.getRoom().getId().equals(roomId))
      throw BusinessException.missing("Occupant not found in this room");
    if (stay.getMoveOutDate() != null)
      throw BusinessException.conflict("Occupant has already left");
    if (moveOutDate.isBefore(stay.getMoveInDate()) || moveOutDate.isAfter(Dates.today()))
      throw BusinessException.invalid("Invalid move-out date");
    stay.setMoveOutDate(moveOutDate);
    return OccupantView.of(stay);
  }
}