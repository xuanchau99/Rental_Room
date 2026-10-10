package com.rental.rental_room.billing;

import com.rental.rental_room.common.Dates;
import com.rental.rental_room.invoice.InvoiceStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BillingLifecycle {
  @PersistenceContext private EntityManager em;

  @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Bangkok")
  @EventListener(ApplicationReadyEvent.class)
  @Transactional
  public void markOverdue() {
    em.createQuery(
            "update Invoice i set i.status=:overdue where i.status in (:unpaid,:partial) and i.dueDate<:today")
        .setParameter("overdue", InvoiceStatus.OVERDUE)
        .setParameter("unpaid", InvoiceStatus.UNPAID)
        .setParameter("partial", InvoiceStatus.PARTIALLY_PAID)
        .setParameter("today", Dates.today())
        .executeUpdate();
  }
}
