package com.rental.rental_room.auth;

import com.rental.rental_room.common.*;
import jakarta.persistence.LockModeType;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final Database db;
  private final PasswordEncoder passwords;
  private final JwtEncoder encoder;
  private final SecureRandom random = new SecureRandom();
  private final String dummyHash;

  public AuthService(Database db, PasswordEncoder passwords, JwtEncoder encoder) {
    this.db = db;
    this.passwords = passwords;
    this.encoder = encoder;
    this.dummyHash = passwords.encode("invalid-login");
  }

  public record Tokens(String accessToken, String refreshToken, String tokenType, long expiresIn) {}

  @Transactional
  public Tokens login(String username, String password) {
    var user =
        db.first(
                "select u from UserAccount u where u.username=:name",
                UserAccount.class,
                "name",
                username)
            .orElse(null);
    boolean matches =
        passwords.matches(password, user == null ? dummyHash : user.getPasswordHash());
    if (user == null || !user.isEnabled() || !matches) throw unauthorized();
    return issue(user);
  }

  @Transactional
  public Tokens refresh(String raw) {
    var query =
        db.em()
            .createQuery(
                "select t from RefreshToken t join fetch t.user where t.tokenHash=:hash",
                RefreshToken.class)
            .setParameter("hash", hash(raw))
            .setLockMode(LockModeType.PESSIMISTIC_WRITE);
    var token = query.getResultStream().findFirst().orElseThrow(AuthService::unauthorized);
    if (token.getRevokedAt() != null
        || token.getExpiresAt().isBefore(Instant.now())
        || !token.getUser().isEnabled()) throw unauthorized();
    token.setRevokedAt(Instant.now());
    return issue(token.getUser());
  }

  @Transactional
  public void logout(String raw) {
    db.first(
            "select t from RefreshToken t where t.tokenHash=:hash",
            RefreshToken.class,
            "hash",
            hash(raw))
        .ifPresent(t -> t.setRevokedAt(Instant.now()));
  }

  private Tokens issue(UserAccount user) {
    Instant now = Instant.now();
    var claims =
        JwtClaimsSet.builder()
            .issuer("rental-room")
            .subject(user.getUsername())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(900))
            .id(UUID.randomUUID().toString())
            .claim("role", user.getRole().name())
            .build();
    String access =
        encoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var token = new RefreshToken();
    token.setUser(user);
    token.setTokenHash(hash(raw));
    token.setExpiresAt(now.plus(Duration.ofDays(7)));
    db.save(token);
    return new Tokens(access, raw, "Bearer", 900);
  }

  static String hash(String raw) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static BusinessException unauthorized() {
    return new BusinessException(HttpStatus.UNAUTHORIZED, "Invalid credentials or refresh token");
  }
}
