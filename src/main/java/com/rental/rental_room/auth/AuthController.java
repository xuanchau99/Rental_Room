package com.rental.rental_room.auth;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor @SecurityRequirements
public class AuthController {
    private final AuthService auth;
    public record Login(@NotBlank @Size(max=100) String username, @NotBlank @Size(max=100) String password) {}
    public record Token(@NotBlank @Size(max=200) String refreshToken) {}
    @PostMapping("/login") public AuthService.Tokens login(@Valid @RequestBody Login r) { return auth.login(r.username(),r.password()); }
    @PostMapping("/refresh") public AuthService.Tokens refresh(@Valid @RequestBody Token r) { return auth.refresh(r.refreshToken()); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody Token r) { auth.logout(r.refreshToken()); }
}
