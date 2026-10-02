package com.rental.rental_room.auth;
import com.rental.rental_room.common.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
@Service @RequiredArgsConstructor
public class UserAdminService {
    private final Database db; private final PasswordEncoder passwords;
    public record View(Long id, String username, UserRole role, boolean enabled) {}
    @Transactional(readOnly=true) public List<View> list() {
        return db.query("select u from UserAccount u order by u.id",UserAccount.class).stream()
            .map(u -> new View(u.getId(),u.getUsername(),u.getRole(),u.isEnabled())).toList();
    }
    @Transactional public View create(String username,String password,UserRole role) {
        var u = new UserAccount();u.setUsername(username);u.setPasswordHash(passwords.encode(password));u.setRole(role);u.setEnabled(true);db.save(u);
        return new View(u.getId(),u.getUsername(),u.getRole(),u.isEnabled());
    }
}
