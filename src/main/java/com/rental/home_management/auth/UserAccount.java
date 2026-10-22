package com.rental.home_management.auth;

import com.rental.home_management.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "user_accounts")
public class UserAccount extends BaseEntity {
  @Column(nullable = false, unique = true, length = 100)
  private String username;

  @Column(nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private UserRole role;

  @Column(nullable = false)
  private boolean enabled;
}
