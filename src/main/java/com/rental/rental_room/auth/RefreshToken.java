package com.rental.rental_room.auth;
import com.rental.rental_room.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;
@Getter @Setter @NoArgsConstructor
@Entity @Table(name = "refresh_tokens")
public class RefreshToken extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column()
    private Instant revokedAt;
}
