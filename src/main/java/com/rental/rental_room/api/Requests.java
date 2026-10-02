package com.rental.rental_room.api;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import com.rental.rental_room.room.RoomStatus;
import com.rental.rental_room.contract.ContractStatus;
import com.rental.rental_room.occupancy.OccupantRole;
import com.rental.rental_room.service.CalculationType;
import com.rental.rental_room.water.WaterCalculationType;
import com.rental.rental_room.payment.PaymentMethod;
import com.rental.rental_room.deposit.DepositType;
public final class Requests {
    private Requests() {}
    public record PropertyInput(@NotBlank @Size(max=150) String name,@Size(max=255) String address) {}
    public record RoomInput(@NotNull Long propertyId,@NotBlank @Size(max=30) String roomNumber,@Size(max=150) String name,
        @NotNull @DecimalMin("0.01") @Digits(integer=6,fraction=2) BigDecimal area,boolean hasLoft,boolean hasAirConditioner,
        @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal monthlyRent,@DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal depositAmount,
        @NotNull RoomStatus status,@Size(max=1000) String description) {}
    public record TenantInput(@NotBlank @Size(max=150) String fullName,@Size(max=30) String phone,
        @Size(max=50) String citizenId,@PastOrPresent LocalDate dateOfBirth,@Size(max=255) String address,
        @Size(max=30) String gender,@Size(max=50) String vehiclePlate,@Size(max=1000) String note) {}
    public record ContractInput(@NotNull Long roomId,@NotNull Long tenantId,@NotNull LocalDate startDate,LocalDate endDate,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal monthlyRent,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal depositAmount,@NotNull ContractStatus status,
        @Min(1) @Max(31) int paymentDueDay,@Size(max=1000) String note,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal electricityPrice,
        @NotNull WaterCalculationType waterCalculationType,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal waterPrice,
        @Size(max=100) List<@Valid Subscription> services) {
        public ContractInput(Long roomId,Long tenantId,LocalDate startDate,LocalDate endDate,BigDecimal monthlyRent,
                BigDecimal depositAmount,ContractStatus status,int paymentDueDay,String note,BigDecimal electricityPrice,
                WaterCalculationType waterCalculationType,BigDecimal waterPrice) {
            this(roomId,tenantId,startDate,endDate,monthlyRent,depositAmount,status,paymentDueDay,note,
                    electricityPrice,waterCalculationType,waterPrice,null);
        }
    }
    public record EndContract(@NotNull LocalDate endDate) {}
    public record RenewContract(@NotNull LocalDate endDate) {}
    public record OccupantInput(@NotNull Long tenantId,@NotNull LocalDate moveInDate) {}
    public record Leave(@NotNull LocalDate moveOutDate) {}
    public record ServiceInput(@NotBlank @Size(max=150) String name,@NotNull CalculationType calculationType,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal price,boolean active) {}
    public record Subscription(@NotNull Long serviceId,@NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal price,
        @NotNull @DecimalMin("0") @Digits(integer=11,fraction=3) BigDecimal quantity) {}
    public record ElectricityInput(@NotNull Long roomId,@Min(1) @Max(12) int month,@Min(2000) @Max(9999) int year,
        @NotNull @DecimalMin("0") @Digits(integer=11,fraction=3) BigDecimal currentReading,
        @DecimalMin("0") @Digits(integer=11,fraction=3) BigDecimal previousReading,@Size(max=1000) String imageUrl) {}
    public record WaterInput(@NotNull Long roomId,@Min(1) @Max(12) int month,@Min(2000) @Max(9999) int year,
        @DecimalMin("0") @Digits(integer=11,fraction=3) BigDecimal currentReading,
        @DecimalMin("0") @Digits(integer=11,fraction=3) BigDecimal previousReading,@Min(0) Integer personCount) {}
    public record Generate(@NotNull Long roomId,@Min(1) @Max(12) int month,@Min(2000) @Max(9999) int year,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal otherAmount,
        @NotNull @DecimalMin("0") @Digits(integer=12,fraction=2) BigDecimal discount) {}
    public record Monthly(Long propertyId,@Min(1) @Max(12) int month,@Min(2000) @Max(9999) int year) {}
    public record Pay(@NotNull @DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal amount,
        @NotNull PaymentMethod paymentMethod,@NotNull @PastOrPresent Instant paymentDate,@Size(max=100) String referenceCode,
        @Size(max=1000) String note) {}
    public record DepositInput(@NotNull DepositType type,@NotNull @DecimalMin("0.01") @Digits(integer=12,fraction=2) BigDecimal amount,
        @NotNull @PastOrPresent LocalDate transactionDate,@Size(max=1000) String note) {}
}
