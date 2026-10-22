package com.rental.home_management.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('OWNER')")
public class UserAdminController {
  private final UserAdminService users;

  public record Create(
      @NotBlank @Size(max = 100) String username,
      @NotBlank @Size(min = 12, max = 72) String password,
      @NotNull UserRole role) {}

  @GetMapping
  public List<UserAdminService.View> list() {
    return users.list();
  }

  @PostMapping
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public UserAdminService.View create(@Valid @RequestBody Create r) {
    return users.create(r.username(), r.password(), r.role());
  }
}
