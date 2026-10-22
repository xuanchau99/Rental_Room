package com.rental.home_management.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'OWNER')")
public class UserAdminController {
  private final UserAdminService users;

  public record Create(
      @NotBlank @Size(max = 100) String username,
      @NotBlank @Size(min = 6, max = 72) String password,
      UserRole role,
      String fullName,
      String phone,
      String note,
      Long ownerId) {}

  public record UpdateProfile(String fullName, String phone, String note) {}

  public record SetStatus(@NotNull Boolean enabled) {}

  public record ResetPassword(@NotBlank @Size(min = 6, max = 72) String newPassword) {}

  @GetMapping
  public List<UserAdminService.View> list(@RequestParam(required = false) UserRole role) {
    return users.list(role);
  }

  @GetMapping("/{id}")
  public UserAdminService.View get(@PathVariable Long id) {
    return users.get(id);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public UserAdminService.View create(@Valid @RequestBody Create r) {
    return users.create(
        r.username(), r.password(), r.role(), r.fullName(), r.phone(), r.note(), r.ownerId());
  }

  @PutMapping("/{id}")
  public UserAdminService.View update(@PathVariable Long id, @Valid @RequestBody UpdateProfile r) {
    return users.update(id, r.fullName(), r.phone(), r.note());
  }

  @PutMapping("/{id}/status")
  public UserAdminService.View setStatus(@PathVariable Long id, @Valid @RequestBody SetStatus r) {
    return users.setStatus(id, r.enabled());
  }

  @PutMapping("/{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPassword r) {
    users.resetPassword(id, r.newPassword());
  }
}
