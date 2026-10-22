package com.rental.home_management.contract;

import com.rental.home_management.common.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
public class ContractLifecycle {
  private static final Logger log = LoggerFactory.getLogger(ContractLifecycle.class);
  private final Database db;
  private final ContractService contracts;

  @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Bangkok")
  @EventListener(ApplicationReadyEvent.class)
  public void expire() {
    var due =
        db.query(
            "select c.id,c.endDate from Contract c where c.status=:active and c.endDate<:today",
            Object[].class,
            "active",
            ContractStatus.ACTIVE,
            "today",
            Dates.today());
    for (var row : due) {
      var id = (Long) row[0];
      try {
        contracts.close(id, (java.time.LocalDate) row[1], ContractStatus.EXPIRED);
      } catch (RuntimeException ex) {
        log.error("Failed to expire contract {}", id, ex);
      }
    }
  }
}
