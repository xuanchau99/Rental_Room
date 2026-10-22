package com.rental.home_management.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public final class SecurityUtils {
  private SecurityUtils() {}

  public static Jwt currentJwt() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
      return jwt;
    }
    return null;
  }

  public static String currentUsername() {
    Jwt jwt = currentJwt();
    return jwt != null ? jwt.getSubject() : null;
  }

  public static UserRole currentRole() {
    Jwt jwt = currentJwt();
    if (jwt != null) {
      String role = jwt.getClaimAsString("role");
      if (role != null) {
        try {
          return UserRole.valueOf(role);
        } catch (IllegalArgumentException ignored) {
        }
      }
    }
    return null;
  }

  public static Long currentUserId() {
    Jwt jwt = currentJwt();
    if (jwt != null) {
      Object claim = jwt.getClaim("userId");
      if (claim instanceof Number n) return n.longValue();
      if (claim instanceof String s) {
        try {
          return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return null;
  }

  public static Long currentOwnerId() {
    Jwt jwt = currentJwt();
    if (jwt != null) {
      Object claim = jwt.getClaim("ownerId");
      if (claim instanceof Number n) return n.longValue();
      if (claim instanceof String s) {
        try {
          return Long.parseLong(s);
        } catch (NumberFormatException ignored) {
        }
      }
    }
    return null;
  }

  public static boolean isSuperAdmin() {
    return currentRole() == UserRole.SUPER_ADMIN;
  }

  public static boolean isOwner() {
    return currentRole() == UserRole.OWNER;
  }

  public static boolean isStaff() {
    return currentRole() == UserRole.STAFF;
  }
}
