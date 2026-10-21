package com.rental.rental_room.service;

import com.rental.rental_room.api.Requests;
import com.rental.rental_room.api.Views.SubscriptionView;
import com.rental.rental_room.common.BusinessException;
import com.rental.rental_room.common.Database;
import com.rental.rental_room.contract.Contract;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractSubscriptionService {
  private final Database db;

  @Transactional(readOnly = true)
  public List<SubscriptionView> list(Long contractId) {
    db.get(Contract.class, contractId);
    return db.query(
        "select s from ContractSubscription s where s.contract.id=:contract order by s.id",
        ContractSubscription.class, "contract", contractId)
        .stream().map(SubscriptionView::of).toList();
  }

  public List<SubscriptionView> replace(Long contractId, List<Requests.Subscription> requests) {
    var contract = db.lock(Contract.class, contractId);
    db.em().createQuery("delete from ContractSubscription s where s.contract.id=:contract")
        .setParameter("contract", contractId).executeUpdate();
    if (requests == null || requests.isEmpty()) return List.of();

    for (var request : requests) {
      var service = db.lock(RentalService.class, request.serviceId());
      if (!service.isActive())
        throw BusinessException.conflict("Inactive service cannot be assigned to a contract");
      var subscription = new ContractSubscription();
      subscription.setContract(contract);
      subscription.setService(service);
      subscription.setPrice(request.price());
      subscription.setQuantity(request.quantity());
      db.save(subscription);
    }
    db.em().flush();
    return list(contractId);
  }
}