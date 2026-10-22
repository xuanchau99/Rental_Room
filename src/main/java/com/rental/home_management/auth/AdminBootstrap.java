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
    UserAccount adminUser;
    if (adminOpt.isPresent()) {
      adminUser = adminOpt.get();
      if (adminUser.getRole() != UserRole.SUPER_ADMIN) {
        adminUser.setRole(UserRole.SUPER_ADMIN);
      }
      if (environment.acceptsProfiles(Profiles.of("local"))) {
        adminUser.setPasswordHash(passwords.encode(properties.admin().password()));
      }
    } else {
      var admin = properties.admin();
      int minimum = environment.acceptsProfiles(Profiles.of("local")) ? 5 : 8;
      if (admin.password().length() < minimum)
        throw new IllegalArgumentException(
            "APP_ADMIN_PASSWORD requires at least " + minimum + " characters");
      adminUser = new UserAccount();
      adminUser.setUsername(admin.username());
      adminUser.setPasswordHash(passwords.encode(admin.password()));
      adminUser.setRole(UserRole.SUPER_ADMIN);
      adminUser.setEnabled(true);
      adminUser.setFullName("Quản Trị Viên Hệ Thống");
      db.save(adminUser);
    }
    db.em()
        .createQuery("update Property p set p.owner = :admin where p.owner is null")
        .setParameter("admin", adminUser)
        .executeUpdate();
    db.em()
        .createQuery("update Tenant t set t.owner = :admin where t.owner is null")
        .setParameter("admin", adminUser)
        .executeUpdate();
  }
}
