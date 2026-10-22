package com.rental.home_management.auth;

import com.rental.home_management.common.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Column(nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private Instant expiresAt;

  @Column() private Instant revokedAt;
}
