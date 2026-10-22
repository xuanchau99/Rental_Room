package com.rental.home_management.dashboard;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  private final DashboardService dashboard;

  @GetMapping
  public DashboardService.Summary summary(
      @RequestParam int month,
      @RequestParam int year,
      @RequestParam(required = false) Long propertyId) {
    return dashboard.summary(month, year, propertyId);
  }
}
