package com.rental.home_management.billing;

import static com.rental.home_management.api.Views.*;

import com.rental.home_management.api.*;
import com.rental.home_management.common.*;
import com.rental.home_management.electricity.*;
import com.rental.home_management.invoice.*;
import com.rental.home_management.room.*;
import com.rental.home_management.water.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReadingService {
  private final Database db;
  private final BillingPolicy policy;

  public ReadingView electricity(Requests.ElectricityInput r) {
    var room = db.lock(Room.class, r.roomId());
    checkRoomOwner(room);
    var p = policy.period(r.month(), r.year());
    var c = policy.contract(room.getId(), p);
    ensureUnbilled(room.getId(), p);
    if (electric(room.getId(), p).isPresent())
      throw BusinessException.conflict("Electricity reading already exists");
    var previous = electric(room.getId(), p.minusMonths(1));
    BigDecimal previousValue =
        previous
            .map(ElectricityMeter::getCurrentReading)
            .orElseGet(() -> initialElectricity(room.getId(), r.previousReading(), p));
    if (previous.isPresent()
        && r.previousReading() != null
        && r.previousReading().compareTo(previousValue) != 0)
      throw BusinessException.invalid("previousReading differs from the previous month");
    BigDecimal usage = usage(previousValue, r.currentReading());
    var e = new ElectricityMeter();
    e.setRoom(room);
    e.setMonth(r.month());
    e.setYear(r.year());
    e.setPreviousReading(previousValue);
    e.setCurrentReading(r.currentReading());
    e.setUsage(usage);
    e.setUnitPrice(c.getElectricityPrice());
    e.setAmount(Money.charge(usage, c.getElectricityPrice()));
    e.setReadingDate(Dates.today());
    e.setReading(r.currentReading());
    e.setImageUrl(r.imageUrl());
    db.save(e);
    return ReadingView.of(e);
  }

  public ReadingView water(Requests.WaterInput r) {
    var room = db.lock(Room.class, r.roomId());
    checkRoomOwner(room);
    var p = policy.period(r.month(), r.year());
    var c = policy.contract(room.getId(), p);
    ensureUnbilled(room.getId(), p);
    if (water(room.getId(), p).isPresent())
      throw BusinessException.conflict("Water reading already exists");
    var e = new WaterMeter();
    e.setRoom(room);
    e.setMonth(r.month());
    e.setYear(r.year());
    e.setCalculationType(c.getWaterCalculationType());
    if (c.getWaterCalculationType() == WaterCalculationType.METER) {
      if (r.currentReading() == null)
        throw BusinessException.invalid("currentReading is required for metered water");
      var previous = water(room.getId(), p.minusMonths(1));
      // A previous per-person record is not a physical meter baseline.
      var meterPrevious =
          previous.filter(v -> v.getCalculationType() == WaterCalculationType.METER);
      BigDecimal pv =
          meterPrevious
              .map(WaterMeter::getCurrentReading)
              .orElseGet(() -> initialWater(room.getId(), r.previousReading(), p));
      if (meterPrevious.isPresent()
          && r.previousReading() != null
          && r.previousReading().compareTo(pv) != 0)
        throw BusinessException.invalid("previousReading differs from previous month");
      e.setPreviousReading(pv);
      e.setCurrentReading(r.currentReading());
      e.setUsage(usage(pv, r.currentReading()));
      e.setPersonCount(0);
      e.setAmount(Money.charge(e.getUsage(), c.getWaterPrice()));
    } else {
      int count = r.personCount() == null ? policy.people(c.getId(), p) : r.personCount();
      if (count < 1)
        throw BusinessException.invalid("A positive personCount is required for per-person water");
      e.setPersonCount(count);
      e.setPreviousReading(BigDecimal.ZERO);
      e.setCurrentReading(BigDecimal.ZERO);
      e.setUsage(BigDecimal.ZERO);
      e.setAmount(Money.charge(BigDecimal.valueOf(count), c.getWaterPrice()));
    }
    e.setUnitPrice(c.getWaterPrice());
    e.setReadingDate(Dates.today());
    e.setReading(e.getCurrentReading());
    db.save(e);
    return ReadingView.of(e);
  }

  private BigDecimal initialElectricity(Long roomId, BigDecimal provided, YearMonth p) {
    boolean hasEarlier =
        db.first(
                "select e from ElectricityMeter e where e.room.id=:room and (e.year<:year or (e.year=:year and e.month<:month))",
                ElectricityMeter.class,
                "room",
                roomId,
                "year",
                p.getYear(),
                "month",
                p.getMonthValue())
            .isPresent();
    if (hasEarlier)
      throw BusinessException.conflict(
          "Previous month is missing; enter missing months before proceeding");
    if (provided == null)
      throw BusinessException.invalid("First meter reading requires previousReading baseline");
    return provided;
  }

  private BigDecimal initialWater(Long roomId, BigDecimal provided, YearMonth p) {
    if (db.first(
            "select e from WaterMeter e where e.room.id=:room and e.calculationType=:mode and (e.year<:year or (e.year=:year and e.month<:month))",
            WaterMeter.class,
            "room",
            roomId,
            "mode",
            WaterCalculationType.METER,
            "year",
            p.getYear(),
            "month",
            p.getMonthValue())
        .isPresent()) throw BusinessException.conflict("Previous metered water month is missing");
    if (provided == null)
      throw BusinessException.invalid(
          "First water meter reading requires previousReading baseline");
    return provided;
  }

  private BigDecimal usage(BigDecimal previous, BigDecimal current) {
    if (current.compareTo(previous) < 0)
      throw BusinessException.invalid("currentReading must be >= previousReading");
    return current.subtract(previous);
  }

  public Optional<ElectricityMeter> electric(Long room, YearMonth p) {
    return db.first(
        "select e from ElectricityMeter e where e.room.id=:room and e.month=:month and e.year=:year",
        ElectricityMeter.class,
        "room",
        room,
        "month",
        p.getMonthValue(),
        "year",
        p.getYear());
  }

  public Optional<WaterMeter> water(Long room, YearMonth p) {
    return db.first(
        "select e from WaterMeter e where e.room.id=:room and e.month=:month and e.year=:year",
        WaterMeter.class,
        "room",
        room,
        "month",
        p.getMonthValue(),
        "year",
        p.getYear());
  }

  void ensureUnbilled(Long room, YearMonth p) {
    if (db.first(
            "select i from Invoice i where i.room.id=:room and i.month=:month and i.year=:year",
            Invoice.class,
            "room",
            room,
            "month",
            p.getMonthValue(),
            "year",
            p.getYear())
        .isPresent())
      throw BusinessException.conflict("This period already has an invoice; readings are locked");
  }

  @Transactional(readOnly = true)
  public List<ReadingView> electricity(
      Long roomId, Integer month, Integer year, int page, int size) {
    if (month != null && (month < 1 || month > 12))
      throw BusinessException.invalid("Invalid month");
    Long ownerId =
        com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
            ? null
            : com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    return db
        .page(
            "select e from ElectricityMeter e where (:ownerId is null or e.room.property.owner.id=:ownerId) and (:room is null or e.room.id=:room) and (:month is null or e.month=:month) and (:year is null or e.year=:year) order by e.year desc,e.month desc,e.id",
            ElectricityMeter.class,
            page,
            size,
            "ownerId",
            ownerId,
            "room",
            roomId,
            "month",
            month,
            "year",
            year)
        .stream()
        .map(ReadingView::of)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ReadingView> water(Long roomId, Integer month, Integer year, int page, int size) {
    if (month != null && (month < 1 || month > 12))
      throw BusinessException.invalid("Invalid month");
    Long ownerId =
        com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
            ? null
            : com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    return db
        .page(
            "select e from WaterMeter e where (:ownerId is null or e.room.property.owner.id=:ownerId) and (:room is null or e.room.id=:room) and (:month is null or e.month=:month) and (:year is null or e.year=:year) order by e.year desc,e.month desc,e.id",
            WaterMeter.class,
            page,
            size,
            "ownerId",
            ownerId,
            "room",
            roomId,
            "month",
            month,
            "year",
            year)
        .stream()
        .map(ReadingView::of)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> worksheet(int month, int year, Long property) {
    var p = policy.period(month, year);
    Long ownerId =
        com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
            ? null
            : com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    var rooms =
        db.query(
            "select r from Room r where (:ownerId is null or r.property.owner.id=:ownerId) and (:property is null or r.property.id=:property) order by r.code",
            Room.class,
            "ownerId",
            ownerId,
            "property",
            property);
    // Fetch all prior readings in two queries, never one query per room.
    var electricity =
        db.query(
            "select e from ElectricityMeter e where (:ownerId is null or e.room.property.owner.id=:ownerId) and e.month=:month and e.year=:year",
            ElectricityMeter.class,
            "ownerId",
            ownerId,
            "month",
            p.minusMonths(1).getMonthValue(),
            "year",
            p.minusMonths(1).getYear());
    var water =
        db.query(
            "select e from WaterMeter e where (:ownerId is null or e.room.property.owner.id=:ownerId) and e.month=:month and e.year=:year",
            WaterMeter.class,
            "ownerId",
            ownerId,
            "month",
            p.minusMonths(1).getMonthValue(),
            "year",
            p.minusMonths(1).getYear());
    var electricByRoom = new HashMap<Long, BigDecimal>();
    electricity.forEach(e -> electricByRoom.put(e.getRoom().getId(), e.getCurrentReading()));
    var waterByRoom = new HashMap<Long, BigDecimal>();
    water.forEach(
        e -> {
          if (e.getCalculationType() == WaterCalculationType.METER)
            waterByRoom.put(e.getRoom().getId(), e.getCurrentReading());
        });
    return rooms.stream()
        .map(
            r -> {
              Map<String, Object> row = new LinkedHashMap<>();
              row.put("roomId", r.getId());
              row.put("roomNumber", r.getCode());
              row.put("previousElectricityReading", electricByRoom.get(r.getId()));
              row.put("previousWaterReading", waterByRoom.get(r.getId()));
              return row;
            })
        .toList();
  }

  private void checkRoomOwner(Room room) {
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()) {
      Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
      if (ownerId != null
          && (room.getProperty().getOwner() == null
              || !room.getProperty().getOwner().getId().equals(ownerId))) {
        throw BusinessException.notFound("Room not found");
      }
    }
  }
}
