package com.rental.rental_room.api;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import com.rental.rental_room.room.*;
import com.rental.rental_room.tenant.*;
import com.rental.rental_room.contract.*;
import com.rental.rental_room.occupancy.*;
import com.rental.rental_room.service.*;
import com.rental.rental_room.water.*;
import com.rental.rental_room.electricity.*;
import com.rental.rental_room.invoice.*;
import com.rental.rental_room.payment.*;
import com.rental.rental_room.deposit.*;
import com.rental.rental_room.property.*;
public final class Views {
    private Views() {}
    public record PropertyView(Long id,String name,String address) {
        public static PropertyView of(Property p) { return new PropertyView(p.getId(),p.getName(),p.getAddress()); }
    }
    public record RoomView(Long id,Long propertyId,String roomNumber,String name,BigDecimal area,boolean hasLoft,
        boolean hasAirConditioner,BigDecimal monthlyRent,BigDecimal depositAmount,RoomStatus status,String description) {
        public static RoomView of(Room r) { return new RoomView(r.getId(),r.getProperty().getId(),r.getCode(),r.getName(),r.getAreaM2(),
            r.isHasMezzanine(),r.isHasAirConditioner(),r.getMonthlyRent(),r.getDepositAmount(),r.getStatus(),r.getDescription()); }
    }
    public record TenantView(Long id,String fullName,String phone,String citizenId,LocalDate dateOfBirth,String address,
        String gender,String vehiclePlate,String note) {
        public static TenantView of(Tenant t) { return new TenantView(t.getId(),t.getFullName(),t.getPhone(),t.getIdentityNumber(),t.getDateOfBirth(),
            t.getAddress(),t.getGender(),t.getVehiclePlate(),t.getNote()); }
    }
    public record ContractView(Long id,Long roomId,Long tenantId,LocalDate startDate,LocalDate endDate,BigDecimal monthlyRent,
        BigDecimal depositAmount,ContractStatus status,int paymentDueDay,String note,BigDecimal electricityPrice,
        WaterCalculationType waterCalculationType,BigDecimal waterPrice) {
        public static ContractView of(Contract c) { return new ContractView(c.getId(),c.getRoom().getId(),c.getTenant().getId(),c.getStartDate(),
            c.getEndDate(),c.getMonthlyRent(),c.getDepositAmount(),c.getStatus(),c.getPaymentDueDay(),c.getNote(),c.getElectricityPrice(),c.getWaterCalculationType(),c.getWaterPrice()); }
    }
    public record OccupantView(Long id,Long roomId,Long tenantId,Long contractId,String fullName,OccupantRole role,LocalDate moveInDate,LocalDate moveOutDate) {
        public static OccupantView of(RoomTenant o) { return new OccupantView(o.getId(),o.getRoom().getId(),o.getTenant().getId(),
            o.getContract().getId(),o.getTenant().getFullName(),o.getRole(),o.getMoveInDate(),o.getMoveOutDate()); }
    }
    public record ServiceView(Long id,String name,CalculationType calculationType,BigDecimal price,boolean active) {
        public static ServiceView of(RentalService s) { return new ServiceView(s.getId(),s.getName(),s.getCalculationType(),s.getPrice(),s.isActive()); }
    }
    public record SubscriptionView(Long id,Long serviceId,String name,CalculationType calculationType,BigDecimal price,BigDecimal quantity) {
        public static SubscriptionView of(com.rental.rental_room.service.ContractService s) { return new SubscriptionView(s.getId(),s.getService().getId(),
            s.getService().getName(),s.getService().getCalculationType(),s.getPrice(),s.getQuantity()); }
    }
    public record ReadingView(Long id,Long roomId,int month,int year,BigDecimal previousReading,BigDecimal currentReading,
        BigDecimal usage,BigDecimal unitPrice,BigDecimal amount,String calculationType,int personCount,String imageUrl) {
        public static ReadingView of(ElectricityMeter e) { return new ReadingView(e.getId(),e.getRoom().getId(),e.getMonth(),e.getYear(),
            e.getPreviousReading(),e.getCurrentReading(),e.getUsage(),e.getUnitPrice(),e.getAmount(),"METER",0,e.getImageUrl()); }
        public static ReadingView of(WaterMeter e) { return new ReadingView(e.getId(),e.getRoom().getId(),e.getMonth(),e.getYear(),
            e.getPreviousReading(),e.getCurrentReading(),e.getUsage(),e.getUnitPrice(),e.getAmount(),e.getCalculationType().name(),e.getPersonCount(),null); }
    }
    public record ItemView(Long id,InvoiceItemType type,String description,BigDecimal quantity,BigDecimal unitPrice,BigDecimal amount) {
        public static ItemView of(InvoiceItem i) { return new ItemView(i.getId(),i.getType(),i.getDescription(),i.getQuantity(),i.getUnitPrice(),i.getAmount()); }
    }
    public record InvoiceView(Long id,Long roomId,Long contractId,String number,int month,int year,BigDecimal roomAmount,
        BigDecimal electricAmount,BigDecimal waterAmount,BigDecimal serviceAmount,BigDecimal otherAmount,BigDecimal discount,
        BigDecimal totalAmount,BigDecimal paidAmount,BigDecimal outstandingAmount,InvoiceStatus status,LocalDate dueDate,Instant paidAt,List<ItemView> items) {}
    public record PaymentView(Long id,Long invoiceId,BigDecimal amount,PaymentMethod paymentMethod,Instant paymentDate,String referenceCode,String note) {
        public static PaymentView of(Payment p) { return new PaymentView(p.getId(),p.getInvoice().getId(),p.getAmount(),p.getMethod(),p.getPaidAt(),p.getReference(),p.getNote()); }
    }
    public record DepositView(Long id,DepositType type,BigDecimal amount,LocalDate transactionDate,String note) {
        public static DepositView of(DepositTransaction t) { return new DepositView(t.getId(),t.getType(),t.getAmount(),t.getTransactionDate(),t.getNote()); }
    }
}
