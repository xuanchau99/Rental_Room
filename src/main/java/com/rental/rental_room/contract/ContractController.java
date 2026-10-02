package com.rental.rental_room.contract;
import com.rental.rental_room.api.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import java.util.*;
import static com.rental.rental_room.api.Views.*;
@RestController @RequestMapping("/api/v1") @RequiredArgsConstructor
public class ContractController {
    private final ContractService contracts;
    @GetMapping("/contracts") public List<ContractView> list(@RequestParam(required=false) Long roomId,@RequestParam(required=false) ContractStatus status,
        @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return contracts.list(roomId,status,page,size); }
    @GetMapping("/contracts/{id}") public ContractView get(@PathVariable Long id) { return contracts.get(id); }
    @PostMapping("/contracts") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @PreAuthorize("hasRole('OWNER')")
    public ContractView create(@Valid @RequestBody Requests.ContractInput r) { return contracts.write(null,r); }
    @PutMapping("/contracts/{id}") @PreAuthorize("hasRole('OWNER')")
    public ContractView update(@PathVariable Long id,@Valid @RequestBody Requests.ContractInput r) { return contracts.write(id,r); }
    @PostMapping("/contracts/{id}/terminate") @PreAuthorize("hasRole('OWNER')")
    public ContractView terminate(@PathVariable Long id,@Valid @RequestBody Requests.EndContract r) { return contracts.terminate(id,r.endDate()); }
    @PostMapping("/contracts/{id}/renew") @PreAuthorize("hasRole('OWNER')")
    public ContractView renew(@PathVariable Long id,@Valid @RequestBody Requests.RenewContract r) { return contracts.renew(id,r.endDate()); }
    @GetMapping("/rooms/{id}/tenants") public List<OccupantView> occupants(@PathVariable Long id) { return contracts.occupants(id); }
    @PostMapping("/rooms/{id}/tenants") public OccupantView addOccupant(@PathVariable Long id,@Valid @RequestBody Requests.OccupantInput r) { return contracts.addOccupant(id,r); }
    @PostMapping("/rooms/{id}/tenants/{stayId}/leave")
    public OccupantView leave(@PathVariable Long id,@PathVariable Long stayId,@Valid @RequestBody Requests.Leave r) { return contracts.leave(id,stayId,r.moveOutDate()); }
    @GetMapping("/contracts/{id}/services") public List<SubscriptionView> subscriptions(@PathVariable Long id) { return contracts.subscriptions(id); }
    @PutMapping("/contracts/{id}/services") @PreAuthorize("hasRole('OWNER')")
    public List<SubscriptionView> subscriptions(@PathVariable Long id,@Valid @RequestBody List<Requests.Subscription> r) { return contracts.subscriptions(id,r); }
    @GetMapping("/contracts/{id}/deposit-transactions") public Map<String,Object> deposits(@PathVariable Long id) { return contracts.deposits(id); }
    @PostMapping("/contracts/{id}/deposit-transactions") @PreAuthorize("hasRole('OWNER')")
    public DepositView deposit(@PathVariable Long id,@Valid @RequestBody Requests.DepositInput r) { return contracts.deposit(id,r); }
}
