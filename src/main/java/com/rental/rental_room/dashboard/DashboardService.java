package com.rental.rental_room.dashboard;
import com.rental.rental_room.common.*;
import com.rental.rental_room.room.*;
import com.rental.rental_room.invoice.*;
import com.rental.rental_room.payment.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;

@Service @RequiredArgsConstructor
public class DashboardService {
    private final Database db;
    public record Debt(Long roomId,String roomNumber,Long invoiceId,BigDecimal outstanding) {}
    public record Summary(int year,int month,long totalRooms,long occupied,long vacant,long maintenance,
        BigDecimal billed,BigDecimal paid,BigDecimal outstanding,BigDecimal rent,BigDecimal electricity,BigDecimal water,
        BigDecimal services,BigDecimal other,BigDecimal discounts,BigDecimal cashReceivedInMonth,List<Debt> debts) {}
    @Transactional(readOnly=true) public Summary summary(int month,int year,Long property) {
        var p=Dates.period(month,year);
        var rooms=db.query("select r from Room r where (:property is null or r.property.id=:property)",Room.class,"property",property);
        long occupied=rooms.stream().filter(r->r.getStatus()==RoomStatus.OCCUPIED).count();
        long vacant=rooms.stream().filter(r->r.getStatus()==RoomStatus.VACANT).count();
        var invoices=db.query("select i from Invoice i join fetch i.room where i.month=:month and i.year=:year and (:property is null or i.room.property.id=:property) and i.status<>:cancel",
            Invoice.class,"month",month,"year",year,"property",property,"cancel",InvoiceStatus.CANCELLED);
        var ids=invoices.stream().map(Invoice::getId).toList();
        var paidByInvoice=new HashMap<Long,BigDecimal>();
        if(!ids.isEmpty())for(var row:db.query("select p.invoice.id,sum(p.amount) from Payment p where p.invoice.id in :ids group by p.invoice.id",
            Object[].class,"ids",ids))paidByInvoice.put((Long)row[0],(BigDecimal)row[1]);
        BigDecimal billed=Money.ZERO,paid=Money.ZERO,rent=Money.ZERO,electric=Money.ZERO,water=Money.ZERO,service=Money.ZERO,other=Money.ZERO,discount=Money.ZERO;
        var debts=new ArrayList<Debt>();
        for(var i:invoices) {
            var amount=paidByInvoice.getOrDefault(i.getId(),Money.ZERO);var remaining=i.getTotalAmount().subtract(amount);
            billed=billed.add(i.getTotalAmount());paid=paid.add(amount);
            rent=rent.add(i.getRoomAmount());electric=electric.add(i.getElectricAmount());water=water.add(i.getWaterAmount());
            service=service.add(i.getServiceAmount());other=other.add(i.getOtherAmount());discount=discount.add(i.getDiscount());
            if(remaining.signum()>0)debts.add(new Debt(i.getRoom().getId(),i.getRoom().getCode(),i.getId(),remaining));
        }
        debts.sort(Comparator.comparing(Debt::roomNumber));
        BigDecimal cash=Money.ZERO;
        var incoming=db.query("select coalesce(sum(p.amount),0) from Payment p where p.paidAt>=:from and p.paidAt<:to and (:property is null or p.invoice.room.property.id=:property)",
            BigDecimal.class,"from",p.atDay(1).atStartOfDay(ZoneId.of("Asia/Bangkok")).toInstant(),
            "to",p.plusMonths(1).atDay(1).atStartOfDay(ZoneId.of("Asia/Bangkok")).toInstant(),"property",property);
        if(!incoming.isEmpty())cash=incoming.getFirst();
        return new Summary(year,month,rooms.size(),occupied,vacant,
            rooms.size()-occupied-vacant,billed,paid,billed.subtract(paid),rent,electric,water,service,other,discount,cash,debts);
    }
}
