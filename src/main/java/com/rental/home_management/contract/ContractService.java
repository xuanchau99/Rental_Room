package com.rental.home_management.contract;

import static com.rental.home_management.api.Views.*;

import com.rental.home_management.api.*;
import com.rental.home_management.common.*;
import com.rental.home_management.deposit.DepositService;
import com.rental.home_management.occupancy.OccupancyService;
import com.rental.home_management.room.*;
import com.rental.home_management.service.ContractSubscriptionService;
import com.rental.home_management.tenant.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {
  private final Database db;
  private final OccupancyService occupancy;
  private final ContractSubscriptionService subscriptions;
  private final DepositService deposits;

  @Transactional(readOnly = true)
  public List<ContractView> list(Long roomId, ContractStatus status, int page, int size) {
    Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    if (com.rental.home_management.auth.SecurityUtils.isSuperAdmin() || ownerId == null) {
      return db
          .page(
              "select c from Contract c where (:room is null or c.room.id=:room) and (:status is null or c.status=:status) order by c.id",
              Contract.class,
              page,
              size,
              "room",
              roomId,
              "status",
              status)
          .stream()
          .map(ContractView::of)
          .toList();
    }
    return db
        .page(
            "select c from Contract c where c.room.property.owner.id=:ownerId and (:room is null or c.room.id=:room) and (:status is null or c.status=:status) order by c.id",
            Contract.class,
            page,
            size,
            "ownerId",
            ownerId,
            "room",
            roomId,
            "status",
            status)
        .stream()
        .map(ContractView::of)
        .toList();
  }

  @Transactional(readOnly = true)
  public ContractView get(Long id) {
    var c = db.get(Contract.class, id);
    Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (c.getRoom().getProperty().getOwner() == null
            || !c.getRoom().getProperty().getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Contract not found");
    }
    return ContractView.of(c);
  }

  public ContractView write(Long id, Requests.ContractInput r) {
    if (r.endDate() != null && r.endDate().isBefore(r.startDate()))
      throw BusinessException.invalid("endDate before startDate");
    if (r.status() != ContractStatus.PENDING && r.status() != ContractStatus.ACTIVE)
      throw BusinessException.invalid("Use terminate/expiration for closing a contract");
    Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    var room = db.lock(Room.class, r.roomId());
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (room.getProperty().getOwner() == null
            || !room.getProperty().getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Room not found");
    }
    var tenant = db.lock(Tenant.class, r.tenantId());
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (tenant.getOwner() == null || !tenant.getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Tenant not found");
    }
    var c = id == null ? new Contract() : db.lock(Contract.class, id);
    if (id != null
        && !com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (c.getRoom().getProperty().getOwner() == null
            || !c.getRoom().getProperty().getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Contract not found");
    }
    if (id != null && c.getStatus() != ContractStatus.PENDING)
      throw BusinessException.conflict(
          "Only pending contracts can be edited; renew or create a new contract");
    if (id != null && !c.getRoom().getId().equals(r.roomId()))
      throw BusinessException.conflict("Contract room cannot be changed");
    if (r.status() == ContractStatus.ACTIVE) {
      if (room.getStatus() != RoomStatus.VACANT)
        throw BusinessException.conflict("Room is occupied or under maintenance");
      if (r.startDate().isAfter(Dates.today())
          || r.endDate() != null && r.endDate().isBefore(Dates.today()))
        throw BusinessException.invalid(
            "Active contract must include today; use PENDING for future start dates");
    }
    c.setRoom(room);
    c.setTenant(tenant);
    c.setStartDate(r.startDate());
    c.setEndDate(r.endDate());
    c.setMonthlyRent(r.monthlyRent());
    c.setDepositAmount(r.depositAmount());
    c.setStatus(r.status());
    c.setPaymentDueDay(r.paymentDueDay());
    c.setNote(r.note());
    c.setElectricityPrice(r.electricityPrice());
    c.setWaterCalculationType(r.waterCalculationType());
    c.setWaterPrice(r.waterPrice());
    if (id == null) db.save(c);
    if (r.status() == ContractStatus.ACTIVE) {
      room.setStatus(RoomStatus.OCCUPIED);
      occupancy.addPrimaryStay(c, r.startDate());
    }
    if (r.services() != null) subscriptions.replace(c.getId(), r.services());
    return ContractView.of(c);
  }

  public ContractView terminate(Long id, LocalDate end) {
    return close(id, end, ContractStatus.TERMINATED);
  }

  public ContractView close(Long id, LocalDate end, ContractStatus status) {
    var initial = db.get(Contract.class, id);
    Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (initial.getRoom().getProperty().getOwner() == null
            || !initial.getRoom().getProperty().getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Contract not found");
    }
    db.lock(Room.class, initial.getRoom().getId());
    var c = db.lock(Contract.class, id);
    if (c.getStatus() != ContractStatus.ACTIVE)
      throw BusinessException.conflict("Contract is not active");
    if (end.isBefore(c.getStartDate()) || end.isAfter(Dates.today()))
      throw BusinessException.invalid("Invalid closing date");
    occupancy.closeStays(c, end);
    c.setEndDate(end);
    c.setStatus(status);
    c.getRoom().setStatus(RoomStatus.VACANT);
    return ContractView.of(c);
  }

  public ContractView renew(Long id, LocalDate end) {
    var initial = db.get(Contract.class, id);
    Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (initial.getRoom().getProperty().getOwner() == null
            || !initial.getRoom().getProperty().getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Contract not found");
    }
    db.lock(Room.class, initial.getRoom().getId());
    var c = db.lock(Contract.class, id);
    if (c.getStatus() != ContractStatus.ACTIVE)
      throw BusinessException.conflict(
          "Only active contracts can be extended; expired contracts require a new contract");
    if (c.getEndDate() == null || !end.isAfter(c.getEndDate()))
      throw BusinessException.invalid("New endDate must extend the existing endDate");
    c.setEndDate(end);
    return ContractView.of(c);
  }

  @Transactional(readOnly = true)
  public List<OccupantView> occupants(Long roomId) {
    var room = db.get(Room.class, roomId);
    Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (room.getProperty().getOwner() == null
            || !room.getProperty().getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Room not found");
    }
    return occupancy.occupants(roomId);
  }

  public OccupantView addOccupant(Long roomId, Requests.OccupantInput request) {
    var room = db.get(Room.class, roomId);
    Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (room.getProperty().getOwner() == null
            || !room.getProperty().getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Room not found");
    }
    return occupancy.addOccupant(roomId, request);
  }

  public OccupantView leave(Long roomId, Long stayId, LocalDate date) {
    var room = db.get(Room.class, roomId);
    Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
        && ownerId != null
        && (room.getProperty().getOwner() == null
            || !room.getProperty().getOwner().getId().equals(ownerId))) {
      throw BusinessException.missing("Room not found");
    }
    return occupancy.leave(roomId, stayId, date);
  }

  @Transactional(readOnly = true)
  public List<SubscriptionView> subscriptions(Long id) {
    return subscriptions.list(id);
  }

  public List<SubscriptionView> subscriptions(Long id, List<Requests.Subscription> requests) {
    return subscriptions.replace(id, requests);
  }

  @Transactional(readOnly = true)
  public Map<String, Object> deposits(Long id) {
    return deposits.summary(id);
  }

  public DepositView deposit(Long id, Requests.DepositInput request) {
    return deposits.record(id, request);
  }
}
