package com.rental.rental_room.auth;
import com.rental.rental_room.common.Database;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
@Component @RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final Database db; private final PasswordEncoder passwords;
    @Value("${app.admin.username}") private String username;
    @Value("${app.admin.password}") private String password;
    @Override @Transactional public void run(ApplicationArguments args) {
        // Serialize bootstrap across backend replicas.
        db.em().createNativeQuery("select pg_advisory_xact_lock(920019)").getSingleResult();
        if (db.query("select count(u) from UserAccount u",Long.class).getFirst()!=0) return;
        if (password.length()<12) throw new IllegalArgumentException("APP_ADMIN_PASSWORD requires at least 12 characters");
        var user = new UserAccount(); user.setUsername(username);user.setPasswordHash(passwords.encode(password));user.setRole(UserRole.OWNER);user.setEnabled(true);db.save(user);
    }
}
