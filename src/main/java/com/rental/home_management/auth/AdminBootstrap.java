package com.rental.home_management.auth;

import com.rental.home_management.common.Database;
import com.rental.home_management.config.AppProperties;
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
    var adminOpt =
        db.first(
            "select u from UserAccount u where u.username=:name",
            UserAccount.class,
            "name",
            properties.admin().username());
    if (adminOpt.isPresent()) {
      var u = adminOpt.get();
      if (u.getRole() != UserRole.SUPER_ADMIN) {
        u.setRole(UserRole.SUPER_ADMIN);
      }
      return;
    }
    if (db.query("select count(u) from UserAccount u", Long.class).getFirst() != 0) return;
    var admin = properties.admin();
    int minimum = environment.acceptsProfiles(Profiles.of("local")) ? 5 : 8;
    if (admin.password().length() < minimum)
      throw new IllegalArgumentException(
          "APP_ADMIN_PASSWORD requires at least " + minimum + " characters");
    var user = new UserAccount();
    user.setUsername(admin.username());
    user.setPasswordHash(passwords.encode(admin.password()));
    user.setRole(UserRole.SUPER_ADMIN);
    user.setEnabled(true);
    user.setFullName("Quản Trị Viên Hệ Thống");
    db.save(user);
  }
}
