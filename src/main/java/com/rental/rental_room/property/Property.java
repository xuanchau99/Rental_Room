package com.rental.rental_room.property;

import com.rental.rental_room.common.BaseEntity;
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
}
