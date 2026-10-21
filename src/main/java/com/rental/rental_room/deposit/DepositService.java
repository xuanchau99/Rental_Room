package com.rental.rental_room.deposit;

import com.rental.rental_room.api.BusinessException;
import com.rental.rental_room.api.Requests;
import com.rental.rental_room.api.Views.DepositView;
import com.rental.rental_room.common.Database;
import com.rental.rental_room.contract.Contract;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DepositService {
  private final Database db;

  @Transactional(readOnly = true)
  public Map<String, Object> summary(Long contractId) {
    var contract = db.get(Contract.class, contractId);
    var transactions = db.query(
        "select t from DepositTransaction t where t.contract.id=:contract order by t.transactionDate, t.id",
        DepositTransaction.class, "contract", contractId);
    BigDecimal received = BigDecimal.ZERO;
    BigDecimal refunded = BigDecimal.ZERO;
    BigDecimal deducted = BigDecimal.ZERO;
    for (var transaction : transactions) {
      switch (transaction.getType()) {
        case RECEIVED -> received = received.add(transaction.getAmount());
        case REFUNDED -> refunded = refunded.add(transaction.getAmount());
        case DEDUCTED -> deducted = deducted.add(transaction.getAmount());
      }
    }
    var result = new LinkedHashMap<String, Object>();
    result.put("contractId", contract.getId());
    result.put("depositAmount", contract.getDepositAmount());
    result.put("received", received);
    result.put("refunded", refunded);
    result.put("deducted", deducted);
    result.put("balance", received.subtract(refunded).subtract(deducted));
    result.put("transactions", transactions.stream().map(DepositView::of).toList());
    return result;
  }

  public DepositView record(Long contractId, Requests.DepositInput request) {
    var contract = db.lock(Contract.class, contractId);
    if (request.type() != DepositType.RECEIVED) {
      var summary = summary(contractId);
      var balance = (BigDecimal) summary.get("balance");
      if (request.amount().compareTo(balance) > 0)
        throw BusinessException.conflict("Deposit refund/deduction exceeds remaining balance");
    }
    var transaction = new DepositTransaction();
    transaction.setContract(contract);
    transaction.setType(request.type());
    transaction.setAmount(request.amount());
    transaction.setTransactionDate(request.transactionDate());
    transaction.setNote(request.note());
    return DepositView.of(db.save(transaction));
  }
}