package com.rental.home_management.api;

import static com.rental.home_management.api.Views.*;

import com.rental.home_management.common.*;
import com.rental.home_management.property.*;
import com.rental.home_management.room.*;
import com.rental.home_management.service.*;
import com.rental.home_management.tenant.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CatalogService {
  private final Database db;
  private final RoomRepository roomRepository;

  @Transactional(readOnly = true)
  public List<PropertyView> properties() {
    return db.query("select p from Property p order by p.id", Property.class).stream()
        .map(PropertyView::of)
        .toList();
  }

  public PropertyView property(Long id, Requests.PropertyInput r) {
    var p = id == null ? new Property() : db.lock(Property.class, id);
    p.setName(r.name());
    p.setAddress(r.address());
    if (id == null) db.save(p);
    return PropertyView.of(p);
  }

  @Transactional(readOnly = true)
  public List<RoomView> rooms(Long propertyId, RoomStatus status, int page, int size) {
    return db
        .page(
            "select r from Room r where (:property is null or r.property.id=:property) and (:status is null or r.status=:status) order by r.id",
            Room.class,
            page,
            size,
            "property",
            propertyId,
            "status",
            status)
        .stream()
        .map(RoomView::of)
        .toList();
  }

  @Transactional(readOnly = true)
  public RoomView room(Long id) {
    return RoomView.of(
        roomRepository.findById(id).orElseThrow(() -> BusinessException.missing("Room not found")));
  }

  public RoomView room(Long id, Requests.RoomInput r) {
    var room = id == null ? new Room() : db.lock(Room.class, id);
    if (id == null && r.status() == RoomStatus.OCCUPIED)
      throw BusinessException.conflict("Use an active contract to occupy a room");
    if (id != null
        && r.status() != room.getStatus()
        && (r.status() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.OCCUPIED))
      throw BusinessException.conflict("Occupied status is controlled by contracts");
    if (id != null && !room.getProperty().getId().equals(r.propertyId()))
      throw BusinessException.conflict("A room cannot move to another property");
    room.setProperty(db.get(Property.class, r.propertyId()));
    room.setCode(r.roomNumber());
    room.setName(r.name());
    room.setAreaM2(r.area());
    room.setHasMezzanine(r.hasLoft());
    room.setHasAirConditioner(r.hasAirConditioner());
    room.setMonthlyRent(r.monthlyRent());
    room.setDepositAmount(r.depositAmount());
    room.setStatus(r.status());
    room.setDescription(r.description());
    if (id == null) db.save(room);
    return RoomView.of(room);
  }

  public void deleteRoom(Long id) {
    var room = db.lock(Room.class, id);
    if (room.getStatus() == RoomStatus.OCCUPIED)
      throw BusinessException.conflict("Occupied room cannot be deleted");
    db.em().remove(room);
    db.em().flush(); // Foreign keys preserve rental/meter/billing history.
  }

  @Transactional(readOnly = true)
  public List<TenantView> tenants(int page, int size) {
    return db.page("select t from Tenant t order by t.id", Tenant.class, page, size).stream()
        .map(TenantView::of)
        .toList();
  }

  @Transactional(readOnly = true)
  public TenantView tenant(Long id) {
    return TenantView.of(db.get(Tenant.class, id));
  }

  public TenantView tenant(Long id, Requests.TenantInput r) {
    var t = id == null ? new Tenant() : db.lock(Tenant.class, id);
    t.setFullName(r.fullName());
    t.setPhone(r.phone());
    t.setIdentityNumber(r.citizenId());
    t.setDateOfBirth(r.dateOfBirth());
    t.setAddress(r.address());
    t.setGender(r.gender());
    t.setVehiclePlate(r.vehiclePlate());
    t.setNote(r.note());
    if (id == null) db.save(t);
    return TenantView.of(t);
  }

  @Transactional(readOnly = true)
  public List<ServiceView> services(int page, int size) {
    return db
        .page("select s from RentalService s order by s.id", RentalService.class, page, size)
        .stream()
        .map(ServiceView::of)
        .toList();
  }

  @Transactional(readOnly = true)
  public ServiceView service(Long id) {
    return ServiceView.of(db.get(RentalService.class, id));
  }

  public ServiceView service(Long id, Requests.ServiceInput r) {
    var s = id == null ? new RentalService() : db.lock(RentalService.class, id);
    s.setName(r.name());
    s.setCalculationType(r.calculationType());
    s.setPrice(r.price());
    s.setActive(r.active());
    if (id == null) db.save(s);
    return ServiceView.of(s);
  }
}
