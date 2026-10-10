package com.rental.rental_room.api;

import static com.rental.rental_room.api.Views.*;

import com.rental.rental_room.room.RoomStatus;
import jakarta.validation.Valid;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CatalogController {
  private final CatalogService catalog;

  @GetMapping("/properties")
  public List<PropertyView> properties() {
    return catalog.properties();
  }

  @PostMapping("/properties")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PreAuthorize("hasRole('OWNER')")
  public PropertyView property(@Valid @RequestBody Requests.PropertyInput r) {
    return catalog.property(null, r);
  }

  @PutMapping("/properties/{id}")
  @PreAuthorize("hasRole('OWNER')")
  public PropertyView property(
      @PathVariable Long id, @Valid @RequestBody Requests.PropertyInput r) {
    return catalog.property(id, r);
  }

  @GetMapping("/rooms")
  public List<RoomView> rooms(
      @RequestParam(required = false) Long propertyId,
      @RequestParam(required = false) RoomStatus status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {
    return catalog.rooms(propertyId, status, page, size);
  }

  @GetMapping("/rooms/{id}")
  public RoomView room(@PathVariable Long id) {
    return catalog.room(id);
  }

  @PostMapping("/rooms")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PreAuthorize("hasRole('OWNER')")
  public RoomView createRoom(@Valid @RequestBody Requests.RoomInput r) {
    return catalog.room(null, r);
  }

  @PutMapping("/rooms/{id}")
  @PreAuthorize("hasRole('OWNER')")
  public RoomView room(@PathVariable Long id, @Valid @RequestBody Requests.RoomInput r) {
    return catalog.room(id, r);
  }

  @DeleteMapping("/rooms/{id}")
  @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
  @PreAuthorize("hasRole('OWNER')")
  public void deleteRoom(@PathVariable Long id) {
    catalog.deleteRoom(id);
  }

  @GetMapping("/tenants")
  public List<TenantView> tenants(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return catalog.tenants(page, size);
  }

  @GetMapping("/tenants/{id}")
  public TenantView tenant(@PathVariable Long id) {
    return catalog.tenant(id);
  }

  @PostMapping("/tenants")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  public TenantView createTenant(@Valid @RequestBody Requests.TenantInput r) {
    return catalog.tenant(null, r);
  }

  @PutMapping("/tenants/{id}")
  public TenantView tenant(@PathVariable Long id, @Valid @RequestBody Requests.TenantInput r) {
    return catalog.tenant(id, r);
  }

  @GetMapping("/services")
  public List<ServiceView> services(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return catalog.services(page, size);
  }

  @GetMapping("/services/{id}")
  public ServiceView service(@PathVariable Long id) {
    return catalog.service(id);
  }

  @PostMapping("/services")
  @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
  @PreAuthorize("hasRole('OWNER')")
  public ServiceView createService(@Valid @RequestBody Requests.ServiceInput r) {
    return catalog.service(null, r);
  }

  @PutMapping("/services/{id}")
  @PreAuthorize("hasRole('OWNER')")
  public ServiceView service(@PathVariable Long id, @Valid @RequestBody Requests.ServiceInput r) {
    return catalog.service(id, r);
  }
}
