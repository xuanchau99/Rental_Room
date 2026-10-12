package com.rental.rental_room.auth;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements
public class AuthController {
  private final AuthService auth;

  public record Login(
      @NotBlank @Size(max = 100) String username, @NotBlank @Size(max = 100) String password) {}

  public record Token(@NotBlank @Size(max = 200) String refreshToken) {}

  public record ChangePassword(
      @NotBlank String currentPassword, @NotBlank @Size(min = 5, max = 72) String newPassword) {}

  @PostMapping("/login")
  public AuthService.Tokens login(@Valid @RequestBody Login r) {
    return auth.login(r.username(), r.password());
  }

  @PostMapping("/refresh")
  public AuthService.Tokens refresh(@Valid @RequestBody Token r) {
    return auth.refresh(r.refreshToken());
  }

  @PostMapping("/change-password")
  public AuthService.Tokens changePassword(
      Authentication authentication, @Valid @RequestBody ChangePassword r) {
    return auth.changePassword(authentication.getName(), r.currentPassword(), r.newPassword());
  }

  @PostMapping("/logout")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void logout(@Valid @RequestBody Token r) {
    auth.logout(r.refreshToken());
  }
}
