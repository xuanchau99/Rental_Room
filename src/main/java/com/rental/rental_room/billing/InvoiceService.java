package com.rental.rental_room.billing;
import com.rental.rental_room.common.*;
import com.rental.rental_room.api.*;
import com.rental.rental_room.contract.*;
import com.rental.rental_room.room.*;
import com.rental.rental_room.invoice.*;
import com.rental.rental_room.payment.*;
import com.rental.rental_room.service.*;
import com.rental.rental_room.tenant.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import static com.rental.rental_room.api.Views.*;

@Service @RequiredArgsConstructor @Transactional
public class InvoiceService {
    private final Database db; private final BillingPolicy policy; private final ReadingService readings;
    public record MonthlyResult(List<Long> created,List<Long> existing,Map<Long,String> failed) {}
    public InvoiceView generate(Requests.Generate r) {
        var room=db.lock(Room.class,r.roomId());var period=policy.period(r.month(),r.year());
        if(existing(room.getId(),period).isPresent())throw BusinessException.conflict("Invoice already exists for room/month/year");
        var c=policy.contract(room.getId(),period);
        var electric=readings.electric(room.getId(),period).orElseThrow(()->BusinessException.conflict("Missing electricity reading"));
        var water=readings.water(room.getId(),period).orElseThrow(()->BusinessException.conflict("Missing water reading"));
        var subs=db.query("select s from ContractService s join fetch s.service where s.contract.id=:id",
            com.rental.rental_room.service.ContractService.class,"id",c.getId());
        var baseRent=policy.rent(c,period);BigDecimal serviceTotal=Money.ZERO;
        var invoice=new Invoice();invoice.setRoom(room);invoice.setContract(c);invoice.setMonth(r.month());invoice.setYear(r.year());
        invoice.setNumber("INV-"+r.year()+"-"+String.format("%02d",r.month())+"-"+room.getId());
        invoice.setPeriodStart(period.atDay(1));invoice.setPeriodEnd(period.atEndOfMonth());
        invoice.setDueDate(period.atDay(Math.min(c.getPaymentDueDay(),period.lengthOfMonth())));
        invoice.setRoomAmount(baseRent);invoice.setElectricAmount(electric.getAmount());invoice.setWaterAmount(water.getAmount());
        invoice.setOtherAmount(r.otherAmount());invoice.setDiscount(r.discount());
        invoice.setStatus(invoice.getDueDate().isBefore(Dates.today())?InvoiceStatus.OVERDUE:InvoiceStatus.UNPAID);
        // Calculate before persisting so a negative total cannot ever be recorded.
        int people=policy.people(c.getId(),period);
        for(var sub:subs) {
            BigDecimal qty=switch(sub.getService().getCalculationType()) {
                case FIXED, PER_VEHICLE -> sub.getQuantity();
                case PER_PERSON -> sub.getQuantity().multiply(BigDecimal.valueOf(people));
                case METER -> serviceUsage(c.getId(),sub.getService().getId(),period);
            };
            serviceTotal=serviceTotal.add(Money.charge(qty,sub.getPrice()));
        }
        invoice.setServiceAmount(serviceTotal);
        var gross=baseRent.add(electric.getAmount()).add(water.getAmount()).add(serviceTotal).add(r.otherAmount());
        if(r.discount().compareTo(gross)>0)throw BusinessException.invalid("Discount exceeds invoice gross amount");
        invoice.setTotalAmount(Money.round(gross.subtract(r.discount())));
        if(invoice.getTotalAmount().signum()==0){invoice.setStatus(InvoiceStatus.PAID);invoice.setPaidAt(Instant.now());}
        db.save(invoice);
        item(invoice,InvoiceItemType.RENT,"Room rent",BigDecimal.ONE,baseRent);
        item(invoice,InvoiceItemType.ELECTRICITY,"Electricity",electric.getUsage(),electric.getUnitPrice());
        if(water.getCalculationType()==com.rental.rental_room.water.WaterCalculationType.METER)
            item(invoice,InvoiceItemType.WATER,"Metered water",water.getUsage(),water.getUnitPrice());
        else item(invoice,InvoiceItemType.WATER,"Water per person",BigDecimal.valueOf(water.getPersonCount()),water.getUnitPrice());
        for(var sub:subs) {
            BigDecimal qty=switch(sub.getService().getCalculationType()) {
                case FIXED, PER_VEHICLE -> sub.getQuantity();
                case PER_PERSON -> sub.getQuantity().multiply(BigDecimal.valueOf(people));
                case METER -> serviceUsage(c.getId(),sub.getService().getId(),period);
            };
            item(invoice,InvoiceItemType.SERVICE,sub.getService().getName(),qty,sub.getPrice());
        }
        if(r.otherAmount().signum()>0)item(invoice,InvoiceItemType.OTHER,"Other charges",BigDecimal.ONE,r.otherAmount());
        // Discounts are represented by an explicit negative item to make the item sum equal the invoice total.
        if(r.discount().signum()>0)discountItem(invoice,r.discount());
        db.em().flush();return get(invoice.getId());
    }
    private BigDecimal serviceUsage(Long contractId,Long serviceId,YearMonth p) {
        return db.first("select u from ServiceUsage u where u.contract.id=:contract and u.service.id=:service and u.month=:month and u.year=:year",
            com.rental.rental_room.service.ServiceUsage.class,"contract",contractId,"service",serviceId,"month",p.getMonthValue(),"year",p.getYear())
            .map(com.rental.rental_room.service.ServiceUsage::getQuantity)
            .orElseThrow(()->BusinessException.conflict("Missing metered service usage"));
    }
    private void item(Invoice invoice,InvoiceItemType type,String label,BigDecimal qty,BigDecimal unit) {
        var i=new InvoiceItem();i.setInvoice(invoice);i.setType(type);i.setDescription(label);i.setQuantity(qty);i.setUnitPrice(unit);i.setAmount(Money.charge(qty,unit));db.save(i);
    }
    private void discountItem(Invoice invoice,BigDecimal discount) {
        var i=new InvoiceItem();i.setInvoice(invoice);i.setType(InvoiceItemType.DISCOUNT);i.setDescription("Discount");
        i.setQuantity(BigDecimal.ONE);i.setUnitPrice(discount.negate());i.setAmount(discount.negate());db.save(i);
    }
    private Optional<Invoice> existing(Long room,YearMonth p) {
        return db.first("select i from Invoice i where i.room.id=:room and i.month=:month and i.year=:year",
            Invoice.class,"room",room,"month",p.getMonthValue(),"year",p.getYear());
    }
    @Transactional(readOnly=true) public InvoiceView get(Long id) {
        var i=db.get(Invoice.class,id);
        var items=db.query("select item from InvoiceItem item where item.invoice.id=:id order by item.id",InvoiceItem.class,"id",id);
        BigDecimal paid=db.query("select coalesce(sum(p.amount),0) from Payment p where p.invoice.id=:id",BigDecimal.class,"id",id).getFirst();
        return view(i,paid,items.stream().map(ItemView::of).toList());
    }
    private InvoiceView view(Invoice i,BigDecimal paid,List<ItemView> items) {
        var due=i.getTotalAmount().subtract(paid);
        var status=i.getStatus()!=InvoiceStatus.PAID&&i.getStatus()!=InvoiceStatus.CANCELLED&&
            i.getDueDate().isBefore(Dates.today())&&due.signum()>0?InvoiceStatus.OVERDUE:i.getStatus();
        return new InvoiceView(i.getId(),i.getRoom().getId(),i.getContract().getId(),i.getNumber(),i.getMonth(),i.getYear(),
            i.getRoomAmount(),i.getElectricAmount(),i.getWaterAmount(),i.getServiceAmount(),i.getOtherAmount(),i.getDiscount(),
            i.getTotalAmount(),paid,due,status,i.getDueDate(),i.getPaidAt(),items);
    }
    @Transactional(readOnly=true) public List<InvoiceView> list(Long roomId,Integer month,Integer year,InvoiceStatus status,int page,int size) {
        if(month!=null&&(month<1||month>12))throw BusinessException.invalid("Invalid month");
        if(page<0 || size<1 || size>100)throw BusinessException.invalid("page >= 0 and size 1..100 required");
        var query="select i from Invoice i where (:room is null or i.room.id=:room) and (:month is null or i.month=:month) and (:year is null or i.year=:year) order by i.year desc,i.month desc,i.id";
        // Effective OVERDUE status depends on the current date. Filter before paging so
        // a matching invoice on a later page is never hidden by a preceding status.
        var invoices=status==null
            ?db.page(query,Invoice.class,page,size,"room",roomId,"month",month,"year",year)
            :db.query(query,Invoice.class,"room",roomId,"month",month,"year",year);
        if(invoices.isEmpty())return List.of();
        var ids=invoices.stream().map(Invoice::getId).toList();
        var payments=db.query("select p.invoice.id, sum(p.amount) from Payment p where p.invoice.id in :ids group by p.invoice.id",Object[].class,"ids",ids);
        var paid=new HashMap<Long,BigDecimal>();for(var row:payments)paid.put((Long)row[0],(BigDecimal)row[1]);
        var result=invoices.stream().map(i->view(i,paid.getOrDefault(i.getId(),Money.ZERO),List.of()))
            .filter(i->status==null||status==i.status()).toList();
        if(status==null)return result;
        long offset=(long)page*size;
        return result.stream().skip(offset).limit(size).toList();
    }
    public PaymentView pay(Long invoiceId,Requests.Pay r) {
        var invoice=db.lock(Invoice.class,invoiceId);
        if(invoice.getStatus()==InvoiceStatus.CANCELLED)throw BusinessException.conflict("Cancelled invoice cannot be paid");
        String ref=r.referenceCode()==null||r.referenceCode().isBlank()?null:r.referenceCode().trim();
        if(ref!=null) {
            var old=db.first("select p from Payment p where p.invoice.id=:id and p.reference=:ref",Payment.class,"id",invoiceId,"ref",ref);
            if(old.isPresent()) {
                var p=old.get();
                if(p.getAmount().compareTo(r.amount())==0 && p.getMethod()==r.paymentMethod() && p.getPaidAt().equals(r.paymentDate()))
                    return PaymentView.of(p);
                throw BusinessException.conflict("Payment reference already used with different data");
            }
        }
        BigDecimal paid=db.query("select coalesce(sum(p.amount),0) from Payment p where p.invoice.id=:id",BigDecimal.class,"id",invoiceId).getFirst();
        BigDecimal next=paid.add(r.amount());
        if(next.compareTo(invoice.getTotalAmount())>0)throw BusinessException.conflict("Payment exceeds outstanding amount");
        if(invoice.getStatus()==InvoiceStatus.PAID)throw BusinessException.conflict("Invoice is already paid");
        var p=new Payment();p.setInvoice(invoice);p.setAmount(r.amount());p.setMethod(r.paymentMethod());p.setPaidAt(r.paymentDate());
        p.setReference(ref);p.setNote(r.note());db.save(p);
        if(next.compareTo(invoice.getTotalAmount())==0){invoice.setStatus(InvoiceStatus.PAID);invoice.setPaidAt(r.paymentDate());}
        else invoice.setStatus(invoice.getDueDate().isBefore(Dates.today())?InvoiceStatus.OVERDUE:InvoiceStatus.PARTIALLY_PAID);
        return PaymentView.of(p);
    }
    @Transactional(readOnly=true) public List<PaymentView> payments(Long id) {
        db.get(Invoice.class,id);
        return db.query("select p from Payment p where p.invoice.id=:id order by p.id",Payment.class,"id",id).stream().map(PaymentView::of).toList();
    }
    public InvoiceView cancel(Long id) {
        var i=db.lock(Invoice.class,id);
        if(!payments(id).isEmpty())throw BusinessException.conflict("Cannot cancel an invoice with payments");
        if(i.getStatus()==InvoiceStatus.PAID||i.getStatus()==InvoiceStatus.CANCELLED)throw BusinessException.conflict("Invoice cannot be cancelled");
        i.setStatus(InvoiceStatus.CANCELLED);return get(id);
    }
    // Each room is independent: one invalid room cannot roll back successful rooms.
    @Transactional(readOnly=true) public List<Room> billableRooms(Long property, YearMonth period) {
        return db.query("select distinct r from Room r join Contract c on c.room=r where (:property is null or r.property.id=:property) and c.status<>:pending and c.startDate<=:last and (c.endDate is null or c.endDate>=:first) order by r.id",
            Room.class,"property",property,"pending",ContractStatus.PENDING,"last",period.atEndOfMonth(),"first",period.atDay(1));
    }
    @Transactional(readOnly=true) public Optional<Long> existingId(Long room,YearMonth p) {return existing(room,p).map(Invoice::getId);}
}
