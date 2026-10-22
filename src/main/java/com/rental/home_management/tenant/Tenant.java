package com.rental.home_management.tenant;

import com.rental.home_management.common.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tenants")
public class Tenant extends BaseEntity {
  @Column(nullable = false, length = 150)
  private String fullName;

  @Column(length = 30)
  private String phone;

  @Column(length = 50)
  private String identityNumber;

  @Column() private LocalDate dateOfBirth;

  @Column(length = 255)
  private String address;

  @Column(length = 30)
  private String gender;

  @Column(length = 50)
  private String vehiclePlate;

  @Column(length = 1000)
  private String note;
}
