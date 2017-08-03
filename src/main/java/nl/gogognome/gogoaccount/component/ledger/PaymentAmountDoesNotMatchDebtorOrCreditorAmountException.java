package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.gogoaccount.services.ServiceException;

public class PaymentAmountDoesNotMatchDebtorOrCreditorAmountException extends ServiceException {

    PaymentAmountDoesNotMatchDebtorOrCreditorAmountException(String message) {
        super(message);
    }
}
