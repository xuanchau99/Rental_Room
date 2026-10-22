package com.rental.home_management.billing;

import static com.rental.home_management.api.Views.*;

import com.rental.home_management.api.*;
import com.rental.home_management.common.*;
import com.rental.home_management.contract.*;
import com.rental.home_management.invoice.*;
import com.rental.home_management.room.*;
import com.rental.home_management.service.*;
import java.math.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceService {
  private final Database db;
  private final BillingPolicy policy;
  private final ReadingService readings;
  private final InvoiceQueryService queries;
  private final InvoicePaymentService paymentService;

  public record MonthlyResult(List<Long> created, List<Long> existing, Map<Long, String> failed) {}

  @Transactional
  public InvoiceView generate(Requests.Generate r) {
    var room = db.lock(Room.class, r.roomId());
    if (!com.rental.home_management.auth.SecurityUtils.isSuperAdmin()) {
      Long ownerId = com.rental.home_management.auth.SecurityUtils.currentOwnerId();
      if (ownerId != null
          && (room.getProperty().getOwner() == null
              || !room.getProperty().getOwner().getId().equals(ownerId))) {
        throw BusinessException.notFound("Room not found");
      }
    }
    var period = policy.period(r.month(), r.year());
    if (existing(room.getId(), period).isPresent())
      throw BusinessException.conflict("Invoice already exists for room/month/year");
    var c = policy.contract(room.getId(), period);
    var electric =
        readings
            .electric(room.getId(), period)
            .orElseThrow(() -> BusinessException.conflict("Missing electricity reading"));
    var water =
        readings
            .water(room.getId(), period)
            .orElseThrow(() -> BusinessException.conflict("Missing water reading"));
    var subs =
        db.query(
            "select s from ContractSubscription s join fetch s.service where s.contract.id=:id",
            ContractSubscription.class,
            "id",
            c.getId());
    var baseRent = policy.rent(c, period);
    var electricAmount = Money.charge(electric.getUsage(), electric.getUnitPrice());
    var waterAmount =
        Money.charge(
            water.getCalculationType()
                    == com.rental.home_management.water.WaterCalculationType.METER
                ? water.getUsage()
                : BigDecimal.valueOf(water.getPersonCount()),
            water.getUnitPrice());
    var otherAmount = Money.round(r.otherAmount());
    var discount = Money.round(r.discount());
    BigDecimal serviceTotal = Money.ZERO;
    var invoice = new Invoice();
    invoice.setRoom(room);
    invoice.setContract(c);
    invoice.setMonth(r.month());
    invoice.setYear(r.year());
    invoice.setNumber(
        "INV-" + r.year() + "-" + String.format("%02d", r.month()) + "-" + room.getId());
    invoice.setPeriodStart(period.atDay(1));
    invoice.setPeriodEnd(period.atEndOfMonth());
    invoice.setDueDate(period.atDay(Math.min(c.getPaymentDueDay(), period.lengthOfMonth())));
    invoice.setRoomAmount(baseRent);
    invoice.setElectricAmount(electricAmount);
    invoice.setWaterAmount(waterAmount);
    invoice.setOtherAmount(otherAmount);
    invoice.setDiscount(discount);
    // Calculate before persisting so a negative total cannot ever be recorded.
    int people = policy.people(c.getId(), period);
    var meteredUsage = new HashMap<Long, BigDecimal>();
    if (subs.stream()
        .anyMatch(sub -> sub.getService().getCalculationType() == CalculationType.METER)) {
      var usages =
          db.query(
              "select u from ServiceUsage u where u.contract.id=:contract and u.month=:month and u.year=:year",
              ServiceUsage.class,
              "contract",
              c.getId(),
              "month",
              period.getMonthValue(),
              "year",
              period.getYear());
      for (var usage : usages) meteredUsage.put(usage.getService().getId(), usage.getQuantity());
    }
    var charges = new ArrayList<ServiceCharge>();
    for (var sub : subs) {
      BigDecimal qty =
          switch (sub.getService().getCalculationType()) {
            case FIXED, PER_VEHICLE -> sub.getQuantity();
            case PER_PERSON -> sub.getQuantity().multiply(BigDecimal.valueOf(people));
            case METER ->
                Optional.ofNullable(meteredUsage.get(sub.getService().getId()))
                    .orElseThrow(() -> BusinessException.conflict("Missing metered service usage"));
          };
      var charge = new ServiceCharge(sub.getService().getName(), qty, sub.getPrice());
      charges.add(charge);
      serviceTotal = serviceTotal.add(charge.amount());
    }
    invoice.setServiceAmount(serviceTotal);
    var gross = baseRent.add(electricAmount).add(waterAmount).add(serviceTotal).add(otherAmount);
    if (discount.compareTo(gross) > 0)
      throw BusinessException.invalid("Discount exceeds invoice gross amount");
    invoice.initializeTotal(gross.subtract(discount), Dates.today());
    db.save(invoice);
    item(invoice, InvoiceItemType.RENT, "Room rent", BigDecimal.ONE, baseRent);
    item(
        invoice,
        InvoiceItemType.ELECTRICITY,
        "Electricity",
        electric.getUsage(),
        electric.getUnitPrice());
    if (water.getCalculationType() == com.rental.home_management.water.WaterCalculationType.METER)
      item(invoice, InvoiceItemType.WATER, "Metered water", water.getUsage(), water.getUnitPrice());
    else
      item(
          invoice,
          InvoiceItemType.WATER,
          "Water per person",
          BigDecimal.valueOf(water.getPersonCount()),
          water.getUnitPrice());
    for (var charge : charges)
      item(invoice, InvoiceItemType.SERVICE, charge.name(), charge.quantity(), charge.price());
    if (otherAmount.signum() > 0)
      item(invoice, InvoiceItemType.OTHER, "Other charges", BigDecimal.ONE, otherAmount);
    // Discounts are represented by an explicit negative item to make the item sum equal the invoice
    // total.
    if (discount.signum() > 0) discountItem(invoice, discount);
    db.em().flush();
    return get(invoice.getId());
  }

  private record ServiceCharge(String name, BigDecimal quantity, BigDecimal price) {
    BigDecimal amount() {
      return Money.charge(quantity, price);
    }
  }

  private void item(
      Invoice invoice, InvoiceItemType type, String label, BigDecimal qty, BigDecimal unit) {
    var i = new InvoiceItem();
    i.setInvoice(invoice);
    i.setType(type);
    i.setDescription(label);
    i.setQuantity(qty);
    i.setUnitPrice(unit);
    i.setAmount(Money.charge(qty, unit));
    db.save(i);
  }

  private void discountItem(Invoice invoice, BigDecimal discount) {
    var i = new InvoiceItem();
    i.setInvoice(invoice);
    i.setType(InvoiceItemType.DISCOUNT);
    i.setDescription("Discount");
    i.setQuantity(BigDecimal.ONE);
    i.setUnitPrice(discount.negate());
    i.setAmount(discount.negate());
    db.save(i);
  }

  private Optional<Invoice> existing(Long room, YearMonth p) {
    return db.first(
        "select i from Invoice i where i.room.id=:room and i.month=:month and i.year=:year",
        Invoice.class,
        "room",
        room,
        "month",
        p.getMonthValue(),
        "year",
        p.getYear());
  }

  public InvoiceView get(Long id) {
    return queries.get(id);
  }

  public List<InvoiceView> list(
      Long roomId, Integer month, Integer year, InvoiceStatus status, int page, int size) {
    return queries.list(roomId, month, year, status, page, size);
  }

  public PaymentView pay(Long invoiceId, Requests.Pay request) {
    return paymentService.pay(invoiceId, request);
  }

  public List<PaymentView> payments(Long id) {
    return paymentService.payments(id);
  }

  public InvoiceView cancel(Long id) {
    return paymentService.cancel(id);
  }

  // Each room is independent: one invalid room cannot roll back successful rooms.
  @Transactional(readOnly = true)
  public List<Room> billableRooms(Long property, YearMonth period) {
    Long ownerId =
        com.rental.home_management.auth.SecurityUtils.isSuperAdmin()
            ? null
            : com.rental.home_management.auth.SecurityUtils.currentOwnerId();
    return db.query(
        "select distinct r from Room r join Contract c on c.room=r "
            + "where (:ownerId is null or r.property.owner.id=:ownerId) "
            + "and (:property is null or r.property.id=:property) and c.status<>:pending "
            + "and c.startDate<=:last and (c.endDate is null or c.endDate>=:first) order by r.id",
        Room.class,
        "ownerId",
        ownerId,
        "property",
        property,
        "pending",
        ContractStatus.PENDING,
        "last",
        period.atEndOfMonth(),
        "first",
        period.atDay(1));
  }

  @Transactional(readOnly = true)
  public Optional<Long> existingId(Long room, YearMonth p) {
    return existing(room, p).map(Invoice::getId);
  }
}
