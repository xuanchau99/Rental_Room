package com.rental.rental_room.contract;
import com.rental.rental_room.common.*;
import com.rental.rental_room.api.*;
import com.rental.rental_room.room.*;
import com.rental.rental_room.tenant.*;
import com.rental.rental_room.occupancy.*;
import com.rental.rental_room.service.RentalService;
import com.rental.rental_room.deposit.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.*;
import java.math.BigDecimal;
import static com.rental.rental_room.api.Views.*;

@Service @RequiredArgsConstructor @Transactional
public class ContractService {
    private final Database db;
    @Transactional(readOnly=true) public List<ContractView> list(Long roomId,ContractStatus status,int page,int size) {
        return db.page("select c from Contract c where (:room is null or c.room.id=:room) and (:status is null or c.status=:status) order by c.id",
            Contract.class,page,size,"room",roomId,"status",status).stream().map(ContractView::of).toList();
    }
    @Transactional(readOnly=true) public ContractView get(Long id) { return ContractView.of(db.get(Contract.class,id)); }
    public ContractView write(Long id,Requests.ContractInput r) {
        if(r.endDate()!=null && r.endDate().isBefore(r.startDate()))throw BusinessException.invalid("endDate before startDate");
        if(r.status()!=ContractStatus.PENDING && r.status()!=ContractStatus.ACTIVE)throw BusinessException.invalid("Use terminate/expiration for closing a contract");
        var room=db.lock(Room.class,r.roomId());
        var c=id==null?new Contract():db.lock(Contract.class,id);
        if(id!=null && c.getStatus()!=ContractStatus.PENDING)throw BusinessException.conflict("Only pending contracts can be edited; renew or create a new contract");
        if(id!=null && !c.getRoom().getId().equals(r.roomId()))throw BusinessException.conflict("Contract room cannot be changed");
        if(r.status()==ContractStatus.ACTIVE) {
            if(room.getStatus()!=RoomStatus.VACANT)throw BusinessException.conflict("Room is occupied or under maintenance");
            if(r.startDate().isAfter(Dates.today()) || r.endDate()!=null && r.endDate().isBefore(Dates.today()))
                throw BusinessException.invalid("Active contract must include today; use PENDING for future start dates");
        }
        c.setRoom(room);c.setTenant(db.lock(Tenant.class,r.tenantId()));c.setStartDate(r.startDate());c.setEndDate(r.endDate());
        c.setMonthlyRent(r.monthlyRent());c.setDepositAmount(r.depositAmount());c.setStatus(r.status());c.setPaymentDueDay(r.paymentDueDay());
        c.setNote(r.note());c.setElectricityPrice(r.electricityPrice());c.setWaterCalculationType(r.waterCalculationType());c.setWaterPrice(r.waterPrice());
        if(id==null)db.save(c);
        if(r.status()==ContractStatus.ACTIVE) { room.setStatus(RoomStatus.OCCUPIED);addStay(c,c.getTenant(),r.startDate(),OccupantRole.PRIMARY_TENANT); }
        if(r.services()!=null) subscriptions(c.getId(),r.services());
        return ContractView.of(c);
    }
    public ContractView terminate(Long id,LocalDate end) { return close(id,end,ContractStatus.TERMINATED); }
    public ContractView close(Long id,LocalDate end,ContractStatus status) {
        var initial=db.get(Contract.class,id); db.lock(Room.class,initial.getRoom().getId());
        var c=db.lock(Contract.class,id);
        if(c.getStatus()!=ContractStatus.ACTIVE)throw BusinessException.conflict("Contract is not active");
        if(end.isBefore(c.getStartDate()) || end.isAfter(Dates.today()))throw BusinessException.invalid("Invalid closing date");
        var stays=db.query("select o from RoomTenant o where o.contract.id=:id and o.moveOutDate is null",RoomTenant.class,"id",id);
        if(stays.stream().anyMatch(o->o.getMoveInDate().isAfter(end)))throw BusinessException.invalid("Closing date precedes an occupant move-in");
        c.setEndDate(end);c.setStatus(status);c.getRoom().setStatus(RoomStatus.VACANT);
        stays.forEach(o->o.setMoveOutDate(end));return ContractView.of(c);
    }
    public ContractView renew(Long id,LocalDate end) {
        var initial=db.get(Contract.class,id);db.lock(Room.class,initial.getRoom().getId());
        var c=db.lock(Contract.class,id);
        if(c.getStatus()!=ContractStatus.ACTIVE)throw BusinessException.conflict("Only active contracts can be extended; expired contracts require a new contract");
        if(c.getEndDate()==null || !end.isAfter(c.getEndDate()))throw BusinessException.invalid("New endDate must extend the existing endDate");
        c.setEndDate(end);return ContractView.of(c);
    }
    @Transactional(readOnly=true) public List<OccupantView> occupants(Long roomId) {
        db.get(Room.class,roomId);
        return db.query("select o from RoomTenant o join fetch o.tenant where o.room.id=:room order by o.moveInDate desc,o.id desc",
            RoomTenant.class,"room",roomId).stream().map(OccupantView::of).toList();
    }
    public OccupantView addOccupant(Long roomId,Requests.OccupantInput r) {
        db.lock(Room.class,roomId);
        var c=db.first("select c from Contract c where c.room.id=:room and c.status=:status",Contract.class,
            "room",roomId,"status",ContractStatus.ACTIVE).orElseThrow(()->BusinessException.conflict("Room has no active contract"));
        if(r.moveInDate().isBefore(c.getStartDate()) || r.moveInDate().isAfter(Dates.today()))
            throw BusinessException.invalid("Invalid move-in date");
        return OccupantView.of(addStay(c,db.lock(Tenant.class,r.tenantId()),r.moveInDate(),OccupantRole.OCCUPANT));
    }
    private RoomTenant addStay(Contract c,Tenant tenant,LocalDate date,OccupantRole role) {
        if(db.first("select o from RoomTenant o where o.tenant.id=:id and o.moveOutDate is null",RoomTenant.class,"id",tenant.getId()).isPresent())
            throw BusinessException.conflict("Tenant already has an active stay; close it before moving rooms");
        var o=new RoomTenant();o.setRoom(c.getRoom());o.setTenant(tenant);o.setContract(c);o.setRole(role);o.setMoveInDate(date);
        return db.save(o);
    }
    public OccupantView leave(Long roomId,Long stayId,LocalDate date) {
        db.lock(Room.class,roomId);var o=db.lock(RoomTenant.class,stayId);
        if(!o.getRoom().getId().equals(roomId))throw BusinessException.missing("Occupant not in this room");
        if(o.getRole()==OccupantRole.PRIMARY_TENANT)throw BusinessException.conflict("Terminate the contract to close the primary tenant stay");
        if(o.getMoveOutDate()!=null || date.isBefore(o.getMoveInDate()) || date.isAfter(Dates.today()))throw BusinessException.invalid("Invalid move-out date");
        o.setMoveOutDate(date);return OccupantView.of(o);
    }
    @Transactional(readOnly=true) public List<SubscriptionView> subscriptions(Long id) {
        db.get(Contract.class,id);
        return db.query("select s from ContractService s join fetch s.service where s.contract.id=:id order by s.id",
            com.rental.rental_room.service.ContractService.class,"id",id).stream().map(SubscriptionView::of).toList();
    }
    public List<SubscriptionView> subscriptions(Long id,List<Requests.Subscription> requests) {
        var initial=db.get(Contract.class,id);db.lock(Room.class,initial.getRoom().getId());var c=db.lock(Contract.class,id);
        if(c.getStatus()!=ContractStatus.ACTIVE && c.getStatus()!=ContractStatus.PENDING)throw BusinessException.conflict("Closed contract");
        if(new HashSet<>(requests.stream().map(Requests.Subscription::serviceId).toList()).size()!=requests.size())throw BusinessException.invalid("Duplicate service");
        db.em().createQuery("delete from ContractService s where s.contract.id=:id").setParameter("id",id).executeUpdate();
        for(var r:requests) {
            var service=db.get(RentalService.class,r.serviceId());if(!service.isActive())throw BusinessException.conflict("Inactive service");
            var s=new com.rental.rental_room.service.ContractService();s.setContract(c);s.setService(service);s.setPrice(r.price());s.setQuantity(r.quantity());db.save(s);
        }
        return subscriptions(id);
    }
    @Transactional(readOnly=true) public Map<String,Object> deposits(Long id) {
        db.get(Contract.class,id);
        var items=db.query("select d from DepositTransaction d where d.contract.id=:id order by d.id",DepositTransaction.class,"id",id);
        return Map.of("balance",balance(items),"transactions",items.stream().map(DepositView::of).toList());
    }
    public DepositView deposit(Long id,Requests.DepositInput r) {
        var c=db.lock(Contract.class,id);
        var items=db.query("select d from DepositTransaction d where d.contract.id=:id",DepositTransaction.class,"id",id);
        if(r.type()!=DepositType.RECEIVED && r.amount().compareTo(balance(items))>0)throw BusinessException.conflict("Deposit refund/deduction exceeds balance");
        var t=new DepositTransaction();t.setContract(c);t.setType(r.type());t.setAmount(r.amount());t.setTransactionDate(r.transactionDate());t.setNote(r.note());
        db.save(t);return DepositView.of(t);
    }
    private BigDecimal balance(List<DepositTransaction> items) {
        return items.stream().map(d->d.getType()==DepositType.RECEIVED?d.getAmount():d.getAmount().negate()).reduce(Money.ZERO,BigDecimal::add);
    }
}
