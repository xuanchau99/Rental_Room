package com.rental.rental_room.auth;

import com.rental.rental_room.common.Database;
import com.rental.rental_room.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
  private final Database db;
  private final PasswordEncoder passwords;
  private final AppProperties properties;
  private final Environment environment;

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    // Serialize bootstrap across backend replicas.
    db.em().createNativeQuery("select pg_advisory_xact_lock(920019)").getSingleResult();
    if (db.query("select count(u) from UserAccount u", Long.class).getFirst() != 0) return;
    var admin = properties.admin();
    int minimum = environment.acceptsProfiles(Profiles.of("local")) ? 5 : 12;
    if (admin.password().length() < minimum)
      throw new IllegalArgumentException(
          "APP_ADMIN_PASSWORD requires at least " + minimum + " characters");
    var user = new UserAccount();
    user.setUsername(admin.username());
    user.setPasswordHash(passwords.encode(admin.password()));
    user.setRole(UserRole.OWNER);
    user.setEnabled(true);
    db.save(user);
  }
}
