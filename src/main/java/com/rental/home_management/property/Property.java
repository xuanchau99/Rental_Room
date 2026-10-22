package com.rental.home_management.property;

import com.rental.home_management.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "properties")
public class Property extends BaseEntity {
  @Column(nullable = false, length = 150)
  private String name;

  @Column(length = 255)
  private String address;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private com.rental.home_management.auth.UserAccount owner;
}
