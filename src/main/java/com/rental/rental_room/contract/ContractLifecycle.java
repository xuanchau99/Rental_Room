package com.rental.rental_room.contract;
import com.rental.rental_room.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import java.util.*;
@Component @EnableScheduling @RequiredArgsConstructor
public class ContractLifecycle {
    private final Database db; private final ContractService contracts;
    @Scheduled(cron="0 5 0 * * *",zone="Asia/Bangkok")
    @EventListener(ApplicationReadyEvent.class)
    public void expire() {
        var ids=db.query("select c.id from Contract c where c.status=:active and c.endDate<:today",Long.class,
            "active",ContractStatus.ACTIVE,"today",Dates.today());
        for(Long id:ids) {
            var c=db.get(Contract.class,id);
            contracts.close(id,c.getEndDate(),ContractStatus.EXPIRED);
        }
    }
}
