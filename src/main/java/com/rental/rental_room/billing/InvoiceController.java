package com.rental.rental_room.billing;

import static com.rental.rental_room.api.Views.*;

import com.rental.rental_room.api.*;
import com.rental.rental_room.invoice.InvoiceStatus;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {
  private final InvoiceService invoices;
  private final MonthlyInvoiceService monthly;

  @PostMapping("/generate")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public InvoiceView generate(@Valid @RequestBody Requests.Generate r) {
    return invoices.generate(r);
  }

  @PostMapping("/generate-monthly")
  public InvoiceService.MonthlyResult generateMonthly(@Valid @RequestBody Requests.Monthly r) {
    return monthly.generate(r);
  }

  @GetMapping
  public List<InvoiceView> list(
      @RequestParam(required = false) Long roomId,
      @RequestParam(required = false) Integer month,
      @RequestParam(required = false) Integer year,
      @RequestParam(required = false) InvoiceStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return invoices.list(roomId, month, year, status, page, size);
  }

  @GetMapping("/{id}")
  public InvoiceView get(@PathVariable Long id) {
    return invoices.get(id);
  }

  @PostMapping("/{id}/payments")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public PaymentView pay(@PathVariable Long id, @Valid @RequestBody Requests.Pay r) {
    return invoices.pay(id, r);
  }

  @GetMapping("/{id}/payments")
  public List<PaymentView> payments(@PathVariable Long id) {
    return invoices.payments(id);
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasRole('OWNER')")
  public InvoiceView cancel(@PathVariable Long id) {
    return invoices.cancel(id);
  }
}
