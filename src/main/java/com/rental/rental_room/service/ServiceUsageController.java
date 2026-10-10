package com.rental.rental_room.service;

import com.rental.rental_room.billing.BillingPolicy;
import com.rental.rental_room.common.*;
import com.rental.rental_room.contract.Contract;
import com.rental.rental_room.invoice.Invoice;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/service-usages")
@RequiredArgsConstructor
public class ServiceUsageController {
  private final UsageService usages;

  public record Input(
      @NotNull Long contractId,
      @NotNull Long serviceId,
      @Min(1) @Max(12) int month,
      @Min(2000) @Max(9999) int year,
      @NotNull @DecimalMin("0") @Digits(integer = 11, fraction = 3) BigDecimal quantity) {}

  public record View(
      Long id, Long contractId, Long serviceId, int month, int year, BigDecimal quantity) {
    static View of(ServiceUsage u) {
      return new View(
          u.getId(),
          u.getContract().getId(),
          u.getService().getId(),
          u.getMonth(),
          u.getYear(),
          u.getQuantity());
    }
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public View create(@Valid @RequestBody Input r) {
    return usages.create(r);
  }

  @GetMapping
  public List<View> list(
      @RequestParam(required = false) Long contractId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return usages.list(contractId, page, size);
  }
}

@Service
@RequiredArgsConstructor
class UsageService {
  private final Database db;
  private final BillingPolicy policy;

  @Transactional
  public ServiceUsageController.View create(ServiceUsageController.Input r) {
    var initial = db.get(Contract.class, r.contractId());
    db.lock(com.rental.rental_room.room.Room.class, initial.getRoom().getId());
    var c = db.lock(Contract.class, r.contractId());
    var period = policy.period(r.month(), r.year());
    if (db.first(
            "select i from Invoice i where i.room.id=:room and i.month=:month and i.year=:year",
            Invoice.class,
            "room",
            c.getRoom().getId(),
            "month",
            r.month(),
            "year",
            r.year())
        .isPresent()) throw BusinessException.conflict("Invoice already issued");
    var service = db.get(RentalService.class, r.serviceId());
    if (service.getCalculationType() != CalculationType.METER)
      throw BusinessException.invalid("Service is not metered");
    if (db.first(
            "select s from ContractSubscription s where s.contract.id=:contract and s.service.id=:service",
            ContractSubscription.class,
            "contract",
            r.contractId(),
            "service",
            r.serviceId())
        .isEmpty()) throw BusinessException.conflict("Service is not subscribed");
    policy.contract(c.getRoom().getId(), period);
    var u = new ServiceUsage();
    u.setContract(c);
    u.setService(service);
    u.setMonth(r.month());
    u.setYear(r.year());
    u.setQuantity(r.quantity());
    db.save(u);
    db.em().flush();
    return ServiceUsageController.View.of(u);
  }

  @Transactional(readOnly = true)
  public List<ServiceUsageController.View> list(Long contract, int page, int size) {
    return db
        .page(
            "select u from ServiceUsage u where (:contract is null or u.contract.id=:contract) order by u.id desc",
            ServiceUsage.class,
            page,
            size,
            "contract",
            contract)
        .stream()
        .map(ServiceUsageController.View::of)
        .toList();
  }
}
