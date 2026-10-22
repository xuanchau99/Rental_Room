package com.rental.home_management.auth;

import com.rental.home_management.common.*;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAdminService {
  private final Database db;
  private final PasswordEncoder passwords;

  public record View(
      Long id,
      String username,
      UserRole role,
      boolean enabled,
      Long ownerId,
      String fullName,
      String phone,
      String note,
      Instant createdAt) {
    public static View of(UserAccount u) {
      return new View(
          u.getId(),
          u.getUsername(),
          u.getRole(),
          u.isEnabled(),
          u.getOwner() != null ? u.getOwner().getId() : null,
          u.getFullName(),
          u.getPhone(),
          u.getNote(),
          u.getCreatedAt());
    }
  }

  @Transactional(readOnly = true)
  public List<View> list(UserRole role) {
    if (SecurityUtils.isSuperAdmin()) {
      return db
          .query(
              "select u from UserAccount u where (:role is null or u.role=:role) order by u.id",
              UserAccount.class,
              "role",
              role)
          .stream()
          .map(View::of)
          .toList();
    }
    if (SecurityUtils.isOwner()) {
      Long currentOwnerId = SecurityUtils.currentOwnerId();
      return db
          .query(
              "select u from UserAccount u where (u.owner.id=:ownerId or u.id=:ownerId) and (:role is null or u.role=:role) order by u.id",
              UserAccount.class,
              "ownerId",
              currentOwnerId,
              "role",
              role)
          .stream()
          .map(View::of)
          .toList();
    }
    throw BusinessException.forbidden("Staff accounts cannot manage users");
  }

  @Transactional(readOnly = true)
  public View get(Long id) {
    var u = db.get(UserAccount.class, id);
    checkAccess(u);
    return View.of(u);
  }

  @Transactional
  public View create(
      String username,
      String password,
      UserRole role,
      String fullName,
      String phone,
      String note,
      Long ownerId) {
    if (db.first(
            "select u.id from UserAccount u where u.username=:name", Long.class, "name", username)
        .isPresent()) {
      throw BusinessException.conflict("Username already exists");
    }

    var u = new UserAccount();
    u.setUsername(username);
    u.setPasswordHash(passwords.encode(password));
    u.setFullName(fullName);
    u.setPhone(phone);
    u.setNote(note);
    u.setEnabled(true);

    if (SecurityUtils.isSuperAdmin()) {
      UserRole targetRole = role != null ? role : UserRole.OWNER;
      u.setRole(targetRole);
      if (targetRole == UserRole.STAFF && ownerId != null) {
        var owner = db.get(UserAccount.class, ownerId);
        u.setOwner(owner);
      }
    } else if (SecurityUtils.isOwner()) {
      if (role != null && role != UserRole.STAFF) {
        throw BusinessException.invalid("Landlords can only create STAFF sub-accounts");
      }
      u.setRole(UserRole.STAFF);
      Long currentOwnerId = SecurityUtils.currentOwnerId();
      var currentOwner = db.get(UserAccount.class, currentOwnerId);
      u.setOwner(currentOwner);
    } else {
      throw BusinessException.forbidden("Staff accounts cannot create users");
    }

    db.save(u);
    return View.of(u);
  }

  @Transactional
  public View update(Long id, String fullName, String phone, String note) {
    var u = db.lock(UserAccount.class, id);
    checkAccess(u);
    u.setFullName(fullName);
    u.setPhone(phone);
    u.setNote(note);
    return View.of(u);
  }

  @Transactional
  public View setStatus(Long id, boolean enabled) {
    var u = db.lock(UserAccount.class, id);
    if (SecurityUtils.isSuperAdmin()) {
      if (u.getUsername().equals(SecurityUtils.currentUsername())) {
        throw BusinessException.invalid("Cannot disable your own administrator account");
      }
      u.setEnabled(enabled);
    } else if (SecurityUtils.isOwner()) {
      if (u.getOwner() == null || !u.getOwner().getId().equals(SecurityUtils.currentOwnerId())) {
        throw BusinessException.notFound("User not found");
      }
      u.setEnabled(enabled);
    } else {
      throw BusinessException.forbidden("Permission denied");
    }
    return View.of(u);
  }

  @Transactional
  public void resetPassword(Long id, String newPassword) {
    var u = db.lock(UserAccount.class, id);
    checkAccess(u);
    u.setPasswordHash(passwords.encode(newPassword));
    var now = Instant.now();
    for (var token :
        db.query(
            "select t from RefreshToken t where t.user.id=:id and t.revokedAt is null",
            RefreshToken.class,
            "id",
            id)) {
      token.setRevokedAt(now);
    }
  }

  private void checkAccess(UserAccount target) {
    if (SecurityUtils.isSuperAdmin()) {
      return;
    }
    if (SecurityUtils.isOwner()) {
      Long currentOwnerId = SecurityUtils.currentOwnerId();
      if (target.getId().equals(currentOwnerId)) {
        return;
      }
      if (target.getOwner() != null && target.getOwner().getId().equals(currentOwnerId)) {
        return;
      }
    }
    throw BusinessException.notFound("User not found");
  }
}
